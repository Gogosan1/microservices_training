package com.example.file.converter.util.exceptions;

public class EventAlreadyProcessingException extends RetryableProcessingException {

    public EventAlreadyProcessingException(String message) {
        super(ProcessingErrorCode.EVENT_ALREADY_PROCESSING, message, null);
    }
}
