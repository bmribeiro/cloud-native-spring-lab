package com.cloudnativespringlab.orderservice;

public record CreateOrderRequest(Long productId, int quantity) {
}
