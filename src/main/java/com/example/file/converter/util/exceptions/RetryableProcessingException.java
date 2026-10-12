package com.example.file.converter.util.exceptions;

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
