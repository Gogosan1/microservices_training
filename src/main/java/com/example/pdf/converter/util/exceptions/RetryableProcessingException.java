package com.example.pdf.converter.util.exceptions;

import com.example.pdf.converter.entity.enums.ProcessingErrorCode;

public class RetryableProcessingException extends RuntimeException {

    private final ProcessingErrorCode errorCode;

    public RetryableProcessingException(
            ProcessingErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ProcessingErrorCode getErrorCode() {
        return errorCode;
    }
}
