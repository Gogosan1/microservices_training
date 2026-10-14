package com.example.pdf.converter.service.persistence;

import com.example.pdf.converter.entity.enums.PersistenceStatus;
import com.example.pdf.converter.repository.InboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxRepository inboxRepository;

    @Transactional
    public boolean tryStartProcessing(UUID eventId) {

        int inserted = inboxRepository.tryAcquire(
                eventId,
                OffsetDateTime.now()
        );

        return inserted == 1;
    }

    @Transactional
    public void markSaved(UUID eventId) {
        updateStatus(eventId, PersistenceStatus.SAVED);
    }

    @Transactional
    public void markFailure(UUID eventId) {
        updateStatus(eventId, PersistenceStatus.FAILURE);
    }

    @Transactional
    public void releaseForRetry(UUID eventId) {
        int updated = inboxRepository.updateStatus(eventId, PersistenceStatus.PROCESSING, PersistenceStatus.RETRY, null);

        if (updated != 1) {
            throw new IllegalStateException("Не удалось освободить событие для повторной попытки: " + eventId);
        }
    }

    private void updateStatus(UUID eventId, PersistenceStatus status) {
        int updated = inboxRepository.updateStatus(
                eventId,
                PersistenceStatus.PROCESSING,
                status,
                OffsetDateTime.now()
        );

        if (updated != 1) {
            throw new IllegalStateException("Невозможно поменять статус для события: " + eventId);
        }
    }


}
