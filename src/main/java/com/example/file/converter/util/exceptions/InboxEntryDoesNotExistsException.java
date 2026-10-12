package com.example.file.converter.util.exceptions;

public class InboxEntryDoesNotExistsException extends RuntimeException {
    public InboxEntryDoesNotExistsException(String message) {
        super(message);
    }
}
