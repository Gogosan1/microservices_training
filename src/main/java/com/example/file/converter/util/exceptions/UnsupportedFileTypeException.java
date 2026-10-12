package com.example.file.converter.util.exceptions;

public class UnsupportedFileTypeException extends PermanentProcessingException {

    public UnsupportedFileTypeException(String message) {
        super(ProcessingErrorCode.UNSUPPORTED_FILE_TYPE, message);
    }
}
