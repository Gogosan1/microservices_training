package com.example.pdf.converter.service.converters;

import com.example.pdf.converter.entity.ConversionResultMetadata;
import com.example.pdf.converter.util.exceptions.InvalidFileContentException;
import com.example.pdf.converter.entity.enums.ProcessingErrorCode;
import com.example.pdf.converter.util.exceptions.RetryableProcessingException;
import org.openpdf.text.Document;
import org.openpdf.text.Image;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.List;

@Component("IMAGE")
public class ImageToPdfConverter implements ConverterStrategy {

    private static final List<String> AVAILABLE_IMAGE_TYPES = List.of("image/jpeg", "image/png");

    @Override
    public ConversionResultMetadata convert(File sourceImageFile, File targetPdfFile) {

        final Image image;
        try {
            URL imageUrl = sourceImageFile.toURI().toURL();
            image = Image.getInstance(imageUrl);
        } catch (Exception exception) {
            throw new InvalidFileContentException(
                    "Изображение не удалось прочитать: файл повреждён или имеет неподдерживаемое содержимое",
                    exception
            );
        }

        Document document = new Document();

        try (FileOutputStream pdfOutputStream = new FileOutputStream(targetPdfFile)
        ) {
            PdfWriter.getInstance(document, pdfOutputStream);
            document.open();

            image.scaleToFit(document.getPageSize().getWidth() - 72,
                    document.getPageSize().getHeight() - 72);

            document.add(image);
            document.close();

            return new ConversionResultMetadata("application/pdf", ".pdf");
        } catch (Exception exception) {
            if (document.isOpen()) {
                try {
                    document.close();
                } catch (Exception closeException) {
                    exception.addSuppressed(closeException);
                }
            }
            throw new RetryableProcessingException(
                    ProcessingErrorCode.CONVERSION_FAILED,
                    "Не удалось сформировать PDF-файл из изображения",
                    exception
            );
        }
    }


    @Override
    public boolean supports(String fileType) {
        return AVAILABLE_IMAGE_TYPES.contains(fileType);
    }
}
