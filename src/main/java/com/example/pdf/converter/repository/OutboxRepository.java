package com.example.pdf.converter.repository;

import com.example.pdf.converter.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
}
