package com.cloudnativespringlab.orderservice;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price) {
}
