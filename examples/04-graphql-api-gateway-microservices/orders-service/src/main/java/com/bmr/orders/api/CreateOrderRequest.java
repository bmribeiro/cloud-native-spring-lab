package com.bmr.orders.api;


import com.bmr.orders.model.OrderStatus;

import java.math.BigDecimal;

public record CreateOrderRequest(
        Long userId,
        BigDecimal total,
        OrderStatus status
) {
}
