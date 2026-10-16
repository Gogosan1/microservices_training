package com.example.pdf.converter.util.exceptions;

import com.example.pdf.converter.entity.enums.ProcessingErrorCode;
import lombok.Getter;

@Getter
public class PermanentProcessingException extends RuntimeException {

    private final ProcessingErrorCode errorCode;
    private final String publicMessage;

    public PermanentProcessingException(ProcessingErrorCode errorCode, String publicMessage) {
        super(publicMessage);
        this.errorCode = errorCode;
        this.publicMessage = publicMessage;
    }

    public PermanentProcessingException(ProcessingErrorCode errorCode, String publicMessage, Throwable cause) {
        super(publicMessage, cause);
        this.errorCode = errorCode;
        this.publicMessage = publicMessage;
    }
}
