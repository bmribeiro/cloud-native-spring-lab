package com.bmr.orders_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(UUID id, String aggregateType, UUID aggregateId, String eventType, String routingKey, String payload) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.payload = payload;
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void registerFailure(String error) {
        this.attempts += 1;
        this.lastError = error;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
