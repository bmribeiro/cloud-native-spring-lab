package com.bmr.orders_api.outbox;

import com.bmr.orders_api.domain.OutboxEventEntity;
import com.bmr.orders_api.domain.OutboxEventRepository;
import com.bmr.orders_api.messaging.RabbitMqNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;
    private final long confirmTimeoutMs;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           RabbitTemplate rabbitTemplate,
                           @Value("${app.outbox.batch-size:25}") int batchSize,
                           @Value("${app.outbox.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> events = outboxEventRepository.findBatchForPublishing(batchSize);

        for (OutboxEventEntity event : events) {
            try {
                publishWithConfirm(event);
                event.markPublished();
                log.info("Published outbox event id={} type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                event.registerFailure(e.getMessage());
                log.error("Failed to publish outbox event id={}", event.getId(), e);
            }
        }
    }

    private void publishWithConfirm(OutboxEventEntity event) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.getId().toString());
        properties.setType(event.getEventType());
        properties.setHeader("event_type", event.getEventType());
        properties.setHeader("aggregate_type", event.getAggregateType());
        properties.setHeader("aggregate_id", event.getAggregateId().toString());

        Message message = new Message(
                event.getPayload().getBytes(StandardCharsets.UTF_8),
                properties
        );

        CorrelationData correlationData = new CorrelationData(event.getId().toString());

        rabbitTemplate.send(
                RabbitMqNames.ORDERS_EXCHANGE,
                event.getRoutingKey(),
                message,
                correlationData
        );

        CorrelationData.Confirm confirm = correlationData
                .getFuture()
                .get(confirmTimeoutMs, TimeUnit.MILLISECONDS);

        if (!confirm.ack()) {
            throw new IllegalStateException("RabbitMQ publish not acknowledged: " + confirm.reason());
        }
    }
}

