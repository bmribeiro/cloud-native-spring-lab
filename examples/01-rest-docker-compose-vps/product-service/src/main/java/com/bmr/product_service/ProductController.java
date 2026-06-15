package com.bmr.product_service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    // Repositório em memória usado para simular uma fonte de dados.
    private final List<Product> products = List.of(
            new Product(1L, "Laptop", new BigDecimal("899.99")),
            new Product(2L, "Monitor", new BigDecimal("199.99")),
            new Product(3L, "Keyboard", new BigDecimal("49.99"))
    );

    // GET /products
    // Devolve HTTP 200 OK com todos os produtos.
    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {
        List<Product> response = products.stream()
                .map(product -> new Product(
                        product.id(),
                        product.name(),
                        product.price()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    // GET /products/{id}
    // Devolve HTTP 200 com o produto quando encontrado, ou HTTP 404 quando inexistente.
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return products.stream()
                .filter(product -> product.id().equals(id))
                .findFirst()
                .map(product -> new Product(
                        product.id(),
                        product.name(),
                        product.price()
                ))
                // Produto encontrado: Devolve HTTP 200 OK
                .map(ResponseEntity::ok)

                // Não encontrado: Devolve HTTP 404 Not Found
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}