package com.bmr.order_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class ProductClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${product.service.url:http://localhost:8081}")
    private String productServiceUrl;

    public ProductDto getProduct(Long id) {
        return restTemplate.getForObject(
                productServiceUrl + "/products/" + id,
                ProductDto.class
        );
    }

    public record ProductDto(
            Long id,
            String name,
            BigDecimal price
    ) {
    }
}