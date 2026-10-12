package com.example.file.converter.repository.database;

import com.example.file.converter.entity.Inbox;
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
    @Query(value = """
            UPDATE converter.inbox
            SET status = :newStatus,
                processed_at_time = :processedAt
            WHERE event_id = :eventId
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int updateProcessingStatus(
            @Param("eventId") UUID eventId,
            @Param("newStatus") String newStatus,
            @Param("processedAt") OffsetDateTime processedAt
    );

    @Modifying
    @Query(value = """
            UPDATE converter.inbox
            SET status = 'RETRY',
                processed_at_time = NULL
            WHERE event_id = :eventId
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int releaseForRetry(@Param("eventId") UUID eventId);

}
