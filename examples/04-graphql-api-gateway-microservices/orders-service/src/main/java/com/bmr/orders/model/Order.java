package com.bmr.orders.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Order(
        Long id,
        Long userId,
        BigDecimal total,
        OrderStatus status,
        OffsetDateTime createdAt
) {
}
