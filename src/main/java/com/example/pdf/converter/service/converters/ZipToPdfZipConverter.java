package com.example.pdf.converter.service.converters;

import com.example.pdf.converter.entity.ConversionResultMetadata;
import com.example.pdf.converter.entity.enums.ProcessingErrorCode;
import com.example.pdf.converter.service.ConverterService;
import com.example.pdf.converter.util.exceptions.InvalidFileContentException;
import com.example.pdf.converter.util.exceptions.PermanentProcessingException;
import com.example.pdf.converter.util.exceptions.RetryableProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.pdf.PdfCopy;
import org.openpdf.text.pdf.PdfImportedPage;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@Slf4j
@Component("ZIP")
@RequiredArgsConstructor
public class ZipToPdfConverter implements ConverterStrategy {

    private static final String AVAILABLE_FILE_TYPE = "application/zip";

    private final ObjectProvider<ConverterService> converterService;

    @Override
    public ConversionResultMetadata convert(
            File sourceZipFile,
            File resultPdfFile
    ) {
        Path temporaryResult = null;

        try {
            Path resultPath = resultPdfFile.toPath().toAbsolutePath();
            Path parentDirectory = resultPath.getParent();

            temporaryResult = Files.createTempFile(parentDirectory,"zip_result_",".pdf");

            int totalPages = convertZipToPdf(
                    sourceZipFile,
                    temporaryResult
            );

            if (totalPages == 0) {
                throw new InvalidFileContentException(
                        "ZIP-архив не содержит файлов, пригодных для конвертации в PDF"
                );
            }

            moveCompletedResult(temporaryResult, resultPath);

            return new ConversionResultMetadata("application/pdf", ".pdf");

        } catch (PermanentProcessingException | RetryableProcessingException exception) {
            throw exception;

        } catch (ZipException exception) {
            throw new InvalidFileContentException(
                    "ZIP-архив повреждён или не может быть прочитан",
                    exception
            );

        } catch (Exception exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.CONVERSION_FAILED,
                    "Не удалось обработать ZIP-архив из-за временной или внутренней ошибки",
                    exception
            );

        } finally {
            if (temporaryResult != null) {
                cleanUpTempFile(temporaryResult.toFile());
            }
        }
    }

    private int convertZipToPdf(File sourceZipFile, Path temporaryResult) throws IOException, DocumentException {

        Document document = new Document();
        int totalPages = 0;

        try (ZipFile zipFile = new ZipFile(sourceZipFile);
             var outputStream = Files.newOutputStream(temporaryResult)) {

            PdfCopy copy = new PdfCopy(document, outputStream);
            Throwable processingFailure = null;

            try {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    if (entry.isDirectory()) {
                        continue;
                    }

                    int addedPages = processSingleZipEntry(
                            zipFile,
                            entry,
                            document,
                            copy
                    );

                    totalPages += addedPages;

                }

                if (totalPages == 0) {
                    throw new InvalidFileContentException(
                            "ZIP-архив не содержит файлов, пригодных для конвертации в PDF"
                    );
                }

                return totalPages;

            } catch (RuntimeException exception) {
                processingFailure = exception;
                throw exception;

            } finally {
                if (document.isOpen()) {
                    try {
                        document.close();
                    } catch (RuntimeException closeException) {
                        if (processingFailure != null) {
                            processingFailure.addSuppressed(closeException);
                        } else {
                            throw closeException;
                        }
                    }
                }
            }
        }
    }


    private int processSingleZipEntry(ZipFile zipFile, ZipEntry entry, Document document, PdfCopy copy) throws IOException {
        File entrySourceFile = null;
        File entryTargetFile = null;

        try {
            entrySourceFile = extractZipEntryToFile(zipFile, entry);
            entryTargetFile = createTempEntryOutputFile();

            converterService.getObject().convert(
                    entrySourceFile,
                    entryTargetFile
            );

            return appendPdf(entryTargetFile, document, copy);

        } finally {
            cleanUpTempFile(entrySourceFile);
            cleanUpTempFile(entryTargetFile);
        }
    }

    private int appendPdf(File sourcePdf, Document destinationDocument, PdfCopy destinationCopy) throws IOException {
        if (!sourcePdf.isFile() || sourcePdf.length() == 0) {
            throw new InvalidFileContentException(
                    "Конвертер вернул пустой PDF для файла: " + sourcePdf.getName()
            );
        }

        PdfReader reader = null;
        boolean readerUsedForCopy = false;

        try {
            reader = new PdfReader(sourcePdf.getAbsolutePath());

            int pageCount = reader.getNumberOfPages();

            if (pageCount == 0) {
                throw new InvalidFileContentException(
                        "Промежуточный PDF не содержит страниц: "
                                + sourcePdf.getName()
                );
            }

            if (!destinationDocument.isOpen()) {
                destinationDocument.open();
            }

            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                PdfImportedPage page = destinationCopy.getImportedPage(
                        reader,
                        pageNumber
                );

                readerUsedForCopy = true;
                destinationCopy.addPage(page);
            }

            return pageCount;

        } catch (IOException | DocumentException exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.CONVERSION_FAILED,
                    "Не удалось добавить страницы промежуточного PDF в итоговый документ",
                    exception
            );

        } finally {
            if (reader != null) {
                try {
                    if (readerUsedForCopy) {
                        destinationCopy.freeReader(reader);
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }

    private File createTempEntryOutputFile() {
        try {
            return Files.createTempFile(
                    "zip_entry_result_",
                    ".pdf"
            ).toFile();

        } catch (IOException exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.TEMP_FILE_IO_FAILED,
                    "Не удалось создать временный файл результата элемента ZIP-архива",
                    exception
            );
        }
    }

    private File extractZipEntryToFile(
            ZipFile zipFile,
            ZipEntry entry
    ) {
        File tempFile = null;

        try {
            tempFile = Files.createTempFile(
                    "zip_entry_",
                    safeTemporarySuffix(entry.getName())
            ).toFile();

            try (var inputStream = zipFile.getInputStream(entry);
                 var outputStream = Files.newOutputStream(tempFile.toPath())) {

                inputStream.transferTo(outputStream);
            }

            return tempFile;

        } catch (IOException exception) {
            cleanUpTempFile(tempFile);

            throw new RetryableProcessingException(
                    ProcessingErrorCode.SOURCE_FILE_READ_FAILED,
                    "Не удалось прочитать элемент ZIP-архива: " + entry.getName(),
                    exception
            );
        }
    }

    private String safeTemporarySuffix(String entryName) {
        int separatorIndex = Math.max(
                entryName.lastIndexOf('/'),
                entryName.lastIndexOf('\\')
        );

        String fileName = entryName.substring(separatorIndex + 1);
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return ".bin";
        }

        String extension = fileName
                .substring(dotIndex)
                .toLowerCase(Locale.ROOT);

        if (extension.length() > 16
                || !extension.matches("\\.[a-z0-9]{1,15}")) {
            return ".bin";
        }

        return extension;
    }

    private void moveCompletedResult(Path temporaryResult, Path resultPath) throws IOException {

        try {
            Files.move(
                    temporaryResult,
                    resultPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    temporaryResult,
                    resultPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void cleanUpTempFile(File file) {
        if (file == null) {
            return;
        }

        try {
            Files.deleteIfExists(file.toPath());

        } catch (IOException exception) {
            log.warn(
                    "Не удалось удалить временный файл: path={}",
                    file.getAbsolutePath(),
                    exception
            );
        }
    }

    @Override
    public boolean supports(String fileType) {
        return AVAILABLE_FILE_TYPE.equals(fileType);
    }
}
