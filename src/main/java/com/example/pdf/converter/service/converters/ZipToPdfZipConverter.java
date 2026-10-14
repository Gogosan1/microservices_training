package com.example.pdf.converter.service.converters;

import com.example.pdf.converter.entity.ConversionResultMetadata;
import com.example.pdf.converter.service.ConverterService;
import com.example.pdf.converter.util.exceptions.InvalidFileContentException;
import com.example.pdf.converter.util.exceptions.PermanentProcessingException;
import com.example.pdf.converter.entity.enums.ProcessingErrorCode;
import com.example.pdf.converter.util.exceptions.RetryableProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Component("ZIP")
@RequiredArgsConstructor
public class ZipToPdfZipConverter implements ConverterStrategy {

    private static final String AVAILABLE_FILE_TYPE = "application/zip";

    private final ObjectProvider<ConverterService> converterService;

    @Override
    public ConversionResultMetadata convert(File sourceZipFile, File resultZipFile) {

        try (ZipFile zipFile = new ZipFile(sourceZipFile);
             FileOutputStream fileOutputStream = new FileOutputStream(resultZipFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    writeDirectoryEntry(zipOutputStream, entry.getName());
                } else {
                    processSingleZipEntry(zipFile, entry, zipOutputStream);
                }
            }

            zipOutputStream.finish();
            return new ConversionResultMetadata("application/zip", ".zip");

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
        }
    }

    private void processSingleZipEntry(ZipFile zipFile, ZipEntry entry, ZipOutputStream zipOutputStream) {
        File entrySourceFile = null;
        File entryTargetFile = null;
        ConversionResultMetadata entryResult = null;

        try {
            entrySourceFile = extractZipEntryToFile(zipFile, entry);
            entryTargetFile = createTempEntryOutputFile();
            entryResult = converterService.getObject().convert(entrySourceFile, entryTargetFile);

            String resultEntryName = replaceExtension(entry.getName(), entryResult.extension());
            writeConvertedFileToZip(zipOutputStream, entryTargetFile, resultEntryName);

        } finally {
            cleanUpTempFile(entrySourceFile);
            cleanUpTempFile(entryTargetFile);
        }
    }

    private File createTempEntryOutputFile() {
        try {
            return Files.createTempFile("zip_entry_result_", ".tmp").toFile();
        } catch (IOException exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.TEMP_FILE_IO_FAILED,
                    "Не удалось создать временный файл результата элемента ZIP-архива",
                    exception
            );
        }
    }

    private String replaceExtension(String entryName, String extension) {
        int slashIndex = Math.max(entryName.lastIndexOf('/'), entryName.lastIndexOf('\\'));
        int dotIndex = entryName.lastIndexOf('.');
        String baseName = dotIndex > slashIndex + 1
                ? entryName.substring(0, dotIndex)
                : entryName;
        return baseName + extension;
    }

    private void writeDirectoryEntry(ZipOutputStream zipOutputStream, String dirName) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(dirName));
        zipOutputStream.closeEntry();
    }

    private File extractZipEntryToFile(ZipFile zipFile, ZipEntry entry) {
        File tempFile = null;
        try {
            tempFile = Files.createTempFile("zip_entry_", safeTemporarySuffix(entry.getName())).toFile();

            try (InputStream entryInputStream = zipFile.getInputStream(entry);
                 FileOutputStream entryFos = new FileOutputStream(tempFile)) {
                entryInputStream.transferTo(entryFos);
            }
            return tempFile;

        } catch (IOException exception) {
            cleanUpTempFile(tempFile);
            throw new RetryableProcessingException(
                    ProcessingErrorCode.SOURCE_FILE_READ_FAILED,
                    "Не удалось прочитать элемент ZIP-архива",
                    exception
            );
        }
    }

    private String safeTemporarySuffix(String entryName) {
        int separatorIndex = Math.max(entryName.lastIndexOf('/'), entryName.lastIndexOf('\\'));
        String fileName = entryName.substring(separatorIndex + 1);
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return ".bin";
        }

        String extension = fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (extension.length() > 16 || !extension.matches("\\.[a-z0-9]{1,15}")) {
            return ".bin";
        }
        return extension;
    }


    private void writeConvertedFileToZip(ZipOutputStream zipOutputStream, File pdfResultFile, String fileName) {
        try {
            zipOutputStream.putNextEntry(new ZipEntry(fileName));
            try (FileInputStream inputStream = new FileInputStream(pdfResultFile)) {
                inputStream.transferTo(zipOutputStream);
            }
            zipOutputStream.closeEntry();

        } catch (IOException exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.TEMP_FILE_IO_FAILED,
                    "Не удалось записать преобразованный файл в результирующий ZIP-архив",
                    exception
            );
        }
    }

    private void cleanUpTempFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    @Override
    public boolean supports(String fileType) {
        return AVAILABLE_FILE_TYPE.equals(fileType);
    }
}