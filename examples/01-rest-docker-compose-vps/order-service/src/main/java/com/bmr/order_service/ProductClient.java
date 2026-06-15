package com.bmr.order_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class ProductClient {

    // Cliente HTTP do Spring usado para fazer chamadas síncronas a outras APIs
    private final RestTemplate restTemplate = new RestTemplate();

    // Injeta a URL base do serviço. Vai buscar às variáveis de ambiente (ex: Docker)
    // ou usa o localhost:8081 como alternativa (fallback) se correr localmente
    @Value("${product.service.url:http://localhost:8081}")
    private String productServiceUrl;

    // Faz um pedido HTTP GET ao endpoint do Product Service e converte
    // automaticamente o JSON recebido num objeto ProductDto
    public ProductDto getProduct(Long id) {
        return restTemplate.getForObject(
                productServiceUrl + "/products/" + id,
                ProductDto.class
        );
    }

    // DTO (Data Transfer Object). Um Record imutável que representa a estrutura
    // esperada do JSON devolvido pelo Product Service.
    public record ProductDto(
            Long id,
            String name,
            BigDecimal price
    ) {
    }
}