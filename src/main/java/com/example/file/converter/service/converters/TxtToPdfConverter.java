package com.example.file.converter.service.converters;

import com.example.file.converter.entity.ConversionResultMetadata;
import com.example.file.converter.util.exceptions.ProcessingErrorCode;
import com.example.file.converter.util.exceptions.RetryableProcessingException;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Component("TXT")
public class TxtToPdfConverter implements ConverterStrategy {

    private static final String AVAILABLE_FILE_TYPE;
    private static final Font RUS_FONT;

    static {
        AVAILABLE_FILE_TYPE = "text/plain";

        try (InputStream is = TxtToPdfConverter.class.getResourceAsStream("/fonts/Roboto-Regular.ttf")) {
            if (is == null) {
                throw new IllegalStateException("Файл шрифта не найден в папке resources/fonts/Roboto-Regular.ttf");
            }
            byte[] fontBytes = is.readAllBytes();
            BaseFont bf = BaseFont.createFont("Roboto-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);

            RUS_FONT = new Font(bf, 12);
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось инициализировать шрифт PDF", exception);
        }
    }

    @Override
    public ConversionResultMetadata convert(File sourceTxtFile, File targetPdfFile) {

        Document document = new Document();

        try (FileOutputStream fileOutputStream = new FileOutputStream(targetPdfFile);
             FileInputStream fileInputStream = new FileInputStream(sourceTxtFile);
             BufferedReader chunkReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8))) {

            PdfWriter.getInstance(document, fileOutputStream);
            document.open();

            String line;
            while ((line = chunkReader.readLine()) != null) {
                document.add(new Paragraph(line, RUS_FONT));
            }

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
                    "Не удалось преобразовать TXT-файл в PDF",
                    exception
            );
        }
    }


    @Override
    public boolean supports(String fileType) {
        return fileType.equals(AVAILABLE_FILE_TYPE);
    }
}
