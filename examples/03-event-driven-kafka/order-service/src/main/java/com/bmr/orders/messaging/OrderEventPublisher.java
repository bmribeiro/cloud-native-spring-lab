package com.bmr.orders.messaging;

import com.bmr.orders.events.OrderCreatedEvent;
import com.bmr.orders.events.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCreatedEvent orderId={}", event.orderId(), ex);
                    } else {
                        log.info("Published OrderCreatedEvent orderId={} topic={} partition={} offset={}",
                                event.orderId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDER_STATUS_CHANGED, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderStatusChangedEvent orderId={}", event.orderId(), ex);
                    } else {
                        log.info("Published OrderStatusChangedEvent orderId={} newStatus={}",
                                event.orderId(), event.newStatus());
                    }
                });
    }
}
