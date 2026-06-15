package com.cloudnativespringlab.productservice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final Map<Long, Product> products = Map.of(
            1L, new Product(1L, "Laptop", new BigDecimal("1299.99")),
            2L, new Product(2L, "Mechanical Keyboard", new BigDecimal("149.90")),
            3L, new Product(3L, "USB-C Dock", new BigDecimal("89.50"))
    );

    @GetMapping
    public Collection<Product> getProducts() {
        return products.values();
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        Product product = products.get(id);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        return product;
    }
}
