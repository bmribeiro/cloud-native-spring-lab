package com.bmr.order_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final ProductClient productClient;

    public OrderController(ProductClient productClient) {
        this.productClient = productClient;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@PathVariable Long id) {
        ProductClient.ProductDto product = productClient.getProduct(1L);

        return Map.of(
                "orderId", id,
                "status", "CREATED",
                "createdAt", Instant.now().toString(),
                "product", product
        );
    }
}