package com.bmr.notifications.messaging;

import com.bmr.notifications.events.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OrderStatusChangedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedListener.class);

    private final ConcurrentMap<UUID, Boolean> processedEvents = new ConcurrentHashMap<>();

    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED, groupId = "notification-service")
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (processedEvents.containsKey(event.eventId())) {
            log.warn("Ignoring duplicated OrderStatusChangedEvent eventId={}", event.eventId());
            return;
        }

        log.info("Notification simulated: orderId={} previousStatus={} newStatus={}",
                event.orderId(),
                event.previousStatus(),
                event.newStatus());

        processedEvents.put(event.eventId(), true);
    }
}
