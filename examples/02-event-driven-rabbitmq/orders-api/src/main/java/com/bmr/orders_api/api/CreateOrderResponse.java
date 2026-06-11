package com.bmr.orders_api.api;

import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        UUID eventId,
        String status
) {
}