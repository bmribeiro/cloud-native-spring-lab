package com.bmr.orders_api.events;

import java.util.UUID;

public record OrderCreatedPayload(
        UUID orderId,
        String customerEmail,
        int amountCents,
        String currency
) {
}