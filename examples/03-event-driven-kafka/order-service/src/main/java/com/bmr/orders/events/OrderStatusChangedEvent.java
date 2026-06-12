package com.bmr.orders.events;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID eventId,
        UUID orderId,
        String previousStatus,
        String newStatus,
        Instant occurredAt
) {
}
