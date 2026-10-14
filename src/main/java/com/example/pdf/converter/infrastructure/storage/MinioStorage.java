package com.example.pdf.converter.infrastructure.storage;

import com.example.pdf.converter.config.properties.MinioAppProperties;
import com.example.pdf.converter.entity.enums.ProcessingErrorCode;
import com.example.pdf.converter.util.exceptions.RetryableProcessingException;
import com.example.pdf.converter.util.exceptions.SourceObjectNotFoundException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class MinioStorage {

    private final MinioClient minioClient;
    private final MinioAppProperties appProperties;

    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(appProperties.bucket())
                            .object(objectKey)
                            .build());
        } catch (ErrorResponseException exception) {
            String errorCode = exception.errorResponse() == null
                    ? null
                    : exception.errorResponse().code();
            if ("NoSuchKey".equals(errorCode) || "NoSuchObject".equals(errorCode)) {
                throw new SourceObjectNotFoundException(objectKey);
            }
            throw new RetryableProcessingException(
                    ProcessingErrorCode.STORAGE_DOWNLOAD_FAILED,
                    "MinIO не смог вернуть исходный объект",
                    exception);
        } catch (Exception exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.STORAGE_DOWNLOAD_FAILED,
                    "Не удалось подключиться к MinIO для скачивания исходного объекта",
                    exception
            );
        }
    }


    public void upload(File file, String targetPath, String contentType) {
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(appProperties.bucket())
                            .object(targetPath)
                            .filename(file.getAbsolutePath())
                            .contentType(contentType)
                            .build());
        } catch (Exception exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.STORAGE_UPLOAD_FAILED,
                    "Не удалось сохранить результирующий объект в MinIO",
                    exception);
        }
    }
}
