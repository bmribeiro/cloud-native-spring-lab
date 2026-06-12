package com.bmr.orders.api;

import com.bmr.orders.model.Order;
import com.bmr.orders.model.OrderStatus;
import com.bmr.orders.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class OrderController {
    private final OrderRepository repository;

    public OrderController(OrderRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "orders-service", "status", "ok");
    }

    @GetMapping("/orders")
    public List<Order> orders(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            return repository.findByUserId(userId);
        }
        return repository.findAll();
    }

    @GetMapping("/orders/{id}")
    public Order order(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        Long userId = requirePositive(request.userId(), "userId");
        BigDecimal total = requireValidTotal(request.total());
        OrderStatus status = request.status() == null ? OrderStatus.PENDING : request.status();

        return repository.create(userId, total, status);
    }

    @PatchMapping("/orders/{id}/status")
    public Order updateOrderStatus(@PathVariable Long id, @RequestBody UpdateOrderStatusRequest request) {
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        return repository.updateStatus(id, request.status())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private static Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be a positive integer");
        }
        return value;
    }

    private static BigDecimal requireValidTotal(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "total must be greater than or equal to zero");
        }
        return value;
    }
}
