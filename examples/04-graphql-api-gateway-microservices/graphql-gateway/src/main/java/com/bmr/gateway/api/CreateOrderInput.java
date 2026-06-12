package com.bmr.gateway.api;

import com.bmr.gateway.model.OrderStatus;

import java.math.BigDecimal;

public record CreateOrderInput(
        Long userId,
        BigDecimal total,
        OrderStatus status
) {
}
