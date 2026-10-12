package com.example.file.converter.util.exceptions;

public class SourceObjectNotFoundException extends PermanentProcessingException {

    public SourceObjectNotFoundException(String objectKey) {
        super(
                ProcessingErrorCode.SOURCE_OBJECT_NOT_FOUND,
                "Исходный объект не найден в MinIO: " + objectKey
        );
    }
}
