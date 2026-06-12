package com.bmr.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Order {
    private final UUID id;
    private final String customerId;
    private final BigDecimal amount;
    private OrderStatus status;
    private final Instant createdAt;

    public Order(UUID id, String customerId, BigDecimal amount, OrderStatus status, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markAsPaid() {
        this.status = OrderStatus.PAID;
    }

    public void reject() {
        this.status = OrderStatus.REJECTED;
    }
}
