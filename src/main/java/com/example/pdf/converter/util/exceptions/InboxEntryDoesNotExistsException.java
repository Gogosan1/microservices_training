package com.example.pdf.converter.util.exceptions;

public class InboxEntryDoesNotExistsException extends RuntimeException {
    public InboxEntryDoesNotExistsException(String message) {
        super(message);
    }
}
