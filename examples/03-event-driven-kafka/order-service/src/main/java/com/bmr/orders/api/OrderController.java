package com.bmr.orders.api;

import com.bmr.orders.service.CreateOrderCommand;
import com.bmr.orders.service.CreateOrderResult;
import com.bmr.orders.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {

        CreateOrderCommand command = new CreateOrderCommand(
                request.customerId(),
                request.amount()
        );

        CreateOrderResult result = orderService.createOrder(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.order().getId())
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location)
                .body(OrderResponse.from(result.order()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID id) {
        return orderService.findById(id)
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
