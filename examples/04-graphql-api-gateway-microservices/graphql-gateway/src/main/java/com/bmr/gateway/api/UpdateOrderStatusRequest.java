package com.bmr.gateway.api;

import com.bmr.gateway.model.OrderStatus;

public record UpdateOrderStatusRequest(
        OrderStatus status
) {
}
