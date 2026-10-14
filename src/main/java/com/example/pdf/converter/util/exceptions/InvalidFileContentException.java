package com.example.pdf.converter.util.exceptions;

import com.example.pdf.converter.entity.enums.ProcessingErrorCode;

public class InvalidFileContentException extends PermanentProcessingException {

    public InvalidFileContentException(String message) {
        super(ProcessingErrorCode.INVALID_FILE_CONTENT, message);
    }

    public InvalidFileContentException(String message, Throwable cause) {
        super(ProcessingErrorCode.INVALID_FILE_CONTENT, message, cause);
    }
}
