package com.bmr.orders_api.service;

import com.bmr.orders_api.api.CreateOrderRequest;
import com.bmr.orders_api.api.CreateOrderResponse;
import com.bmr.orders_api.domain.OrderEntity;
import com.bmr.orders_api.domain.OrderRepository;
import com.bmr.orders_api.domain.OutboxEventEntity;
import com.bmr.orders_api.domain.OutboxEventRepository;
import com.bmr.orders_api.events.EventEnvelope;
import com.bmr.orders_api.events.OrderCreatedPayload;
import com.bmr.orders_api.messaging.RabbitMqNames;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;
    private final EntityManager entityManager;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxEventRepository,
                        JsonMapper jsonMapper,
                        EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {

        OrderEntity order = new OrderEntity(
                request.customerEmail(),
                request.amountCents(),
                request.currency()
        );

        orderRepository.save(order);

        UUID eventId = UUID.randomUUID();

        OrderCreatedPayload payload = new OrderCreatedPayload(
                order.getId(),
                order.getCustomerEmail(),
                order.getAmountCents(),
                order.getCurrency()
        );

        EventEnvelope<OrderCreatedPayload> event = new EventEnvelope<>(
                eventId,
                "order.created.v1",
                1,
                Instant.now(),
                payload
        );

        String payloadJson = toJson(event);

        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                eventId,
                "order",
                order.getId(),
                event.eventType(),
                RabbitMqNames.ORDER_CREATED_ROUTING_KEY,
                payloadJson
        );

        outboxEventRepository.save(outboxEvent);

        // Força o Hibernate/JPA a executar os INSERTs
        entityManager.flush();

        return new CreateOrderResponse(order.getId(), eventId, "ACCEPTED");
    }

    private String toJson(EventEnvelope<OrderCreatedPayload> event) {
        return jsonMapper.writeValueAsString(event);
    }
}
