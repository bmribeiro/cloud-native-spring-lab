package com.bmr.notifications_worker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
public class NotificationLogEntity {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationLogEntity() {
    }

    public NotificationLogEntity(UUID orderId, UUID eventId, String recipientEmail, String status, String message) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.eventId = eventId;
        this.recipientEmail = recipientEmail;
        this.status = status;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
