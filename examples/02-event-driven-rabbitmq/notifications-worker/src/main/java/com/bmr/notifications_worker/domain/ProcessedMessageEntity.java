package com.bmr.notifications_worker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_messages")
public class ProcessedMessageEntity {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedMessageEntity() {
    }

    public ProcessedMessageEntity(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
