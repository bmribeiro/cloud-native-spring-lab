package com.bmr.notifications_worker.events;

import java.util.UUID;

public record OrderCreatedPayload(
        UUID orderId,
        String customerEmail,
        int amountCents,
        String currency
) {
}
