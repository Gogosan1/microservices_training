package com.example.pdf.converter.util.exceptions;

import com.example.pdf.converter.entity.enums.ProcessingErrorCode;

public class UnsupportedFileTypeException extends PermanentProcessingException {

    public UnsupportedFileTypeException(String message) {
        super(ProcessingErrorCode.UNSUPPORTED_FILE_TYPE, message);
    }
}
