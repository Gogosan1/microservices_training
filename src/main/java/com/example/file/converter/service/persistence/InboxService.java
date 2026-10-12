package com.example.file.converter.service.persistence;

import com.example.file.converter.entity.PersistenceStatuses;
import com.example.file.converter.repository.database.InboxRepository;
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
        updateStatus(eventId, PersistenceStatuses.SAVED);
    }

    @Transactional
    public void markFailure(UUID eventId) {
        updateStatus(eventId, PersistenceStatuses.FAILURE);
    }

    @Transactional
    public void releaseForRetry(UUID eventId) {
        int updated = inboxRepository.releaseForRetry(eventId);

        if (updated != 1) {
            throw new IllegalStateException("Не удалось освободить событие для повторной попытки: " + eventId);
        }
    }

    private void updateStatus(UUID eventId, PersistenceStatuses status) {
        int updated = inboxRepository.updateProcessingStatus(
                eventId,
                status.name(),
                OffsetDateTime.now()
        );

        if (updated != 1) {
            throw new IllegalStateException("Невозможно поменять статус для события: " + eventId);
        }
    }


}
