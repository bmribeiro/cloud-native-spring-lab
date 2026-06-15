package com.cloudnativespringlab.orderservice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final RestClient productServiceClient;
    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<Long, OrderResponse> orders = new ConcurrentHashMap<>();

    public OrderController(RestClient productServiceClient) {
        this.productServiceClient = productServiceClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        validateRequest(request);

        ProductResponse product = getProduct(request.productId());
        BigDecimal totalPrice = product.price().multiply(BigDecimal.valueOf(request.quantity()));

        Long orderId = sequence.getAndIncrement();
        OrderResponse order = new OrderResponse(orderId, product, request.quantity(), totalPrice);
        orders.put(orderId, order);

        return order;
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        OrderResponse order = orders.get(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id);
        }
        return order;
    }

    private ProductResponse getProduct(Long productId) {
        try {
            return productServiceClient
                    .get()
                    .uri("/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found: " + productId);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable");
        }
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request == null || request.productId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        if (request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be greater than zero");
        }
    }
}
