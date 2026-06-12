package com.bmr.payments.messaging;

import com.bmr.payments.events.OrderCreatedEvent;
import com.bmr.payments.events.PaymentResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
    private static final BigDecimal PAYMENT_LIMIT = new BigDecimal("1000.00");

    private final PaymentEventPublisher publisher;
    private final ConcurrentMap<UUID, Boolean> processedEvents = new ConcurrentHashMap<>();

    public OrderCreatedListener(PaymentEventPublisher publisher) {
        this.publisher = publisher;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "payment-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        if (processedEvents.containsKey(event.eventId())) {
            log.warn("Ignoring duplicated OrderCreatedEvent eventId={}", event.eventId());
            return;
        }

        if ("fail".equalsIgnoreCase(event.customerId())) {
            throw new IllegalStateException("Forced failure for DLT study, orderId=" + event.orderId());
        }

        log.info("Received OrderCreatedEvent orderId={} customerId={} amount={}",
                event.orderId(), event.customerId(), event.amount());

        PaymentResultEvent result;

        if (event.amount().compareTo(PAYMENT_LIMIT) <= 0) {
            result = new PaymentResultEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    "AUTHORIZED",
                    null,
                    Instant.now()
            );
        } else {
            result = new PaymentResultEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    "REJECTED",
                    "Amount exceeds payment limit",
                    Instant.now()
            );
        }

        publisher.publish(result);
        processedEvents.put(event.eventId(), true);
    }
}
