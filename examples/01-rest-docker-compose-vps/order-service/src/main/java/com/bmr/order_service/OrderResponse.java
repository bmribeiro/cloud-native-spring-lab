package com.bmr.order_service;

public record OrderResponse(
        Long orderId,
        String status,
        String createdAt,
        ProductClient.ProductDto product
) {
}