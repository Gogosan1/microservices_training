package com.example.file.converter.service.persistence;

import com.example.file.converter.util.exceptions.PermanentProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final InboxService inboxService;
    private final OutboxService outboxService;

    public boolean tryStartProcessing(UUID eventId) {
        return inboxService.tryStartProcessing(eventId);
    }

    @Transactional
    public void success(UUID eventId, String newPdfFilePath) {

        inboxService.markSaved(eventId);
        outboxService.saveSuccessEvent(eventId, newPdfFilePath);
    }

    @Transactional
    public void failure(UUID eventId, PermanentProcessingException exception) {

        inboxService.markFailure(eventId);
        outboxService.saveFailureEvent(eventId, exception);
    }

    public void releaseForRetry(UUID eventId) {
        inboxService.releaseForRetry(eventId);
    }

}
