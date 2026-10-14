package com.example.pdf.converter.util.exceptions;

import com.example.pdf.converter.entity.enums.ProcessingErrorCode;

public class EventAlreadyProcessingException extends RetryableProcessingException {

    public EventAlreadyProcessingException(String message) {
        super(ProcessingErrorCode.EVENT_ALREADY_PROCESSING, message, null);
    }
}
