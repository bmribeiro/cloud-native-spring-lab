package com.cloudnativespringlab.orderservice;

import java.math.BigDecimal;

public record OrderResponse(Long id, ProductResponse product, int quantity, BigDecimal totalPrice) {
}
