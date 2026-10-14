package com.example.pdf.converter.entity;

import com.example.pdf.converter.entity.enums.PersistenceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Inbox {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    private PersistenceStatus status;

    private OffsetDateTime createdAtTime;
    private OffsetDateTime processedAtTime;
}