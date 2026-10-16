package com.example.pdf.converter.service.persistence;

import com.example.pdf.converter.util.exceptions.PermanentProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final InboxService inboxService;
    private final OutboxService outboxService;

    public boolean tryRegisterInInbox(UUID eventId) {
        return inboxService.tryStartProcessing(eventId);
    }

    @Transactional
    public void success(UUID eventId, String newPdfFilePath) {

        inboxService.markSaved(eventId);
        outboxService.saveSuccessEvent(eventId, newPdfFilePath);
    }

    @Transactional
    public void markAsPermanentFailure(UUID eventId, PermanentProcessingException exception) {

        inboxService.markFailure(eventId);
        outboxService.saveFailureEvent(eventId, exception);
    }

    public void releaseForRetry(UUID eventId) {
        inboxService.releaseForRetry(eventId);
    }

}
