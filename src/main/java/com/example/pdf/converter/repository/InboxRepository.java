package com.example.pdf.converter.repository;

import com.example.pdf.converter.entity.Inbox;
import com.example.pdf.converter.entity.enums.PersistenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface InboxRepository extends JpaRepository<Inbox, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO converter.inbox AS existing (
                event_id,
                status,
                created_at_time,
                processed_at_time
            )
            VALUES (
                :eventId,
                'PROCESSING',
                :now,
                NULL
            )
            ON CONFLICT (event_id) DO UPDATE
            SET status = 'PROCESSING',
                processed_at_time = NULL
            WHERE existing.status = 'RETRY'
            """, nativeQuery = true)
    int tryAcquire(@Param("eventId") UUID eventId,
                   @Param("now") OffsetDateTime now);


    @Modifying
    @Query("""
            UPDATE Inbox i 
            SET i.status = :newStatus,
                i.processedAtTime = :processedAt
            WHERE i.eventId = :eventId
            AND i.status = :expectedStatus
            """)
    int updateStatus(
            @Param("eventId") UUID eventId,
            @Param("expectedStatus") PersistenceStatus expectedStatus,
            @Param("newStatus") PersistenceStatus newStatus,
            @Param("processedAt") OffsetDateTime processedAt
    );
}
