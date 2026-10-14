package com.example.pdf.converter.util.exceptions;

import com.example.pdf.converter.entity.enums.ProcessingErrorCode;

public class InvalidInputEventException extends PermanentProcessingException {

    public InvalidInputEventException(String message) {
        super(ProcessingErrorCode.INVALID_EVENT, message);
    }
}
