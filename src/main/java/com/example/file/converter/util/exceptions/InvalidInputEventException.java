package com.example.file.converter.util.exceptions;

public class InvalidInputEventException extends PermanentProcessingException {

    public InvalidInputEventException(String message) {
        super(ProcessingErrorCode.INVALID_EVENT, message);
    }
}
