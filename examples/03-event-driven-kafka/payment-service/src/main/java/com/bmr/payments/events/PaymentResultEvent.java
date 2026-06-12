package com.bmr.payments.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentResultEvent(
        UUID eventId,
        UUID orderId,
        String status,
        String reason,
        Instant occurredAt
) {
}
