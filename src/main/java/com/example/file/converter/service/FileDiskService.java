package com.example.file.converter.service;

import com.example.file.converter.repository.storage.MinioStorage;
import com.example.file.converter.util.exceptions.ProcessingErrorCode;
import com.example.file.converter.util.exceptions.RetryableProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class FileDiskService {

    public File downloadToTempFile(String pathToFile, MinioStorage storage) throws Exception {
        File tempFile = File.createTempFile("minio_incoming_", ".tmp");
        try (InputStream inputStream = storage.download(pathToFile);
             FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            inputStream.transferTo(fileOutputStream);
        }
        log.info("Файл из S3:{} скачан во временный файл:{}", pathToFile, tempFile.toPath());
        return tempFile;
    }

    public File createTempOutputFile() {
        try {
            return File.createTempFile("converted_output_", ".tmp");
        } catch (IOException exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.TEMP_FILE_IO_FAILED,
                    "Не удалось создать временный файл результата конвертации",
                    exception
            );
        }
    }

    public void cleanUpTempFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}
