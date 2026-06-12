package com.bmr.orders.service;

import java.math.BigDecimal;

public record CreateOrderCommand(
        String customerId,
        BigDecimal amount
) {}
