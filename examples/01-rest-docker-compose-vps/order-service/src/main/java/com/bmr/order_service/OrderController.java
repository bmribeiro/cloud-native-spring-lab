package com.bmr.order_service;

import org.springframework.http.ResponseEntity;
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

    // Construtor usado pelo Spring para injetar a dependência (Dependency Injection)
    public OrderController(ProductClient productClient) {
        this.productClient = productClient;
    }

    // GET /orders/{id}
    // Devolve uma encomenda simulada com o respetivo produto associado.
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {

        // Através do ProductClient (comunicação entre microserviços)
        ProductClient.ProductDto product = productClient.getProduct(1L);

        // Constrói a resposta final agrupando os dados da encomenda com os dados do produto
        OrderResponse response = new OrderResponse(
                id,
                "CREATED",
                Instant.now().toString(),
                product
        );

        // Retorna HTTP 200 OK com o JSON da encomenda
        return ResponseEntity.ok(response);
    }
}