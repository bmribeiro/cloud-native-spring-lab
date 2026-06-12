package com.bmr.orders.api;

import com.bmr.orders.model.OrderStatus;

public record UpdateOrderStatusRequest(
        OrderStatus status
) {
}
