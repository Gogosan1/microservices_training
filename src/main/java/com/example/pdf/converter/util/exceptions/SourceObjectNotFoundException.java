package com.example.pdf.converter.util.exceptions;

import com.example.pdf.converter.entity.enums.ProcessingErrorCode;

public class SourceObjectNotFoundException extends PermanentProcessingException {

    public SourceObjectNotFoundException(String objectKey) {
        super(
                ProcessingErrorCode.SOURCE_OBJECT_NOT_FOUND,
                "Исходный объект не найден в MinIO: " + objectKey
        );
    }
}
