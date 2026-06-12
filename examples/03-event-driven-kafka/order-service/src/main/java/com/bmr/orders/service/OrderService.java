package com.bmr.orders.service;

import com.bmr.orders.domain.Order;
import com.bmr.orders.domain.OrderStatus;
import com.bmr.orders.events.OrderCreatedEvent;
import com.bmr.orders.messaging.OrderEventPublisher;
import com.bmr.orders.store.InMemoryOrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final InMemoryOrderRepository repository;
    private final OrderEventPublisher publisher;

    public OrderService(InMemoryOrderRepository repository, OrderEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public CreateOrderResult createOrder(CreateOrderCommand command) {
        Order order = new Order(
                UUID.randomUUID(),
                command.customerId(),
                command.amount(),
                OrderStatus.PENDING_PAYMENT,
                Instant.now()
        );

        repository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCustomerId(),
                order.getAmount(),
                Instant.now()
        );

        publisher.publishOrderCreated(event);

        return new CreateOrderResult(order);
    }

    public Optional<Order> findById(UUID id) {
        return repository.findById(id);
    }
}
