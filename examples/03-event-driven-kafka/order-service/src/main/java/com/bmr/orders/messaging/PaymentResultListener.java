package com.bmr.orders.messaging;

import com.bmr.orders.domain.OrderStatus;
import com.bmr.orders.events.OrderStatusChangedEvent;
import com.bmr.orders.events.PaymentResultEvent;
import com.bmr.orders.store.InMemoryOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final InMemoryOrderRepository repository;
    private final OrderEventPublisher publisher;
    private final ConcurrentMap<UUID, Boolean> processedEvents = new ConcurrentHashMap<>();

    public PaymentResultListener(InMemoryOrderRepository repository, OrderEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_RESULT, groupId = "order-service")
    public void onPaymentResult(PaymentResultEvent event) {
        if (processedEvents.containsKey(event.eventId())) {
            log.warn("Ignoring duplicated PaymentResultEvent eventId={}", event.eventId());
            return;
        }

        log.info("Received PaymentResultEvent orderId={} status={} reason={}",
                event.orderId(), event.status(), event.reason());

        var order = repository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found for PaymentResultEvent orderId=" + event.orderId()));

        OrderStatus previousStatus = order.getStatus();

        if ("AUTHORIZED".equals(event.status())) {
            order.markAsPaid();
        } else if ("REJECTED".equals(event.status())) {
            order.reject();
        } else {
            throw new IllegalArgumentException("Unknown payment status: " + event.status());
        }

        repository.save(order);

        OrderStatusChangedEvent statusChangedEvent = new OrderStatusChangedEvent(
                UUID.randomUUID(),
                order.getId(),
                previousStatus.name(),
                order.getStatus().name(),
                Instant.now()
        );

        publisher.publishOrderStatusChanged(statusChangedEvent);
        processedEvents.put(event.eventId(), true);
    }
}
