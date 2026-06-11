package com.bmr.notifications_worker.consumer;

import com.bmr.notifications_worker.events.OrderCreatedPayload;
import com.bmr.notifications_worker.messaging.RabbitMqNames;
import com.bmr.notifications_worker.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderCreatedListener {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final JsonMapper jsonMapper;
    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;
    private final int maxRetries;

    public OrderCreatedListener(JsonMapper jsonMapper,
                                NotificationService notificationService,
                                RabbitTemplate rabbitTemplate,
                                @Value("${app.worker.max-retries:3}") int maxRetries) {
        this.jsonMapper = jsonMapper;
        this.notificationService = notificationService;
        this.rabbitTemplate = rabbitTemplate;
        this.maxRetries = maxRetries;
    }

    @RabbitListener(queues = RabbitMqNames.ORDER_CREATED_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            JsonNode root = jsonMapper.readTree(message.getBody());

            UUID eventId = UUID.fromString(root.get("eventId").asText());
            String eventType = root.get("eventType").asText();

            if (!"order.created.v1".equals(eventType)) {
                throw new IllegalArgumentException("Unsupported event type: " + eventType);
            }

            OrderCreatedPayload payload = jsonMapper.treeToValue(
                    root.get("payload"),
                    OrderCreatedPayload.class
            );

            notificationService.handleOrderCreated(eventId, payload);
            channel.basicAck(deliveryTag, false);

            log.info("Processed and acked eventId={}", eventId);

        } catch (Exception e) {
            int retryCount = getRetryCountFromMainQueue(message);

            log.error(
                    "Failed to process messageId={} retryCount={} maxRetries={}",
                    message.getMessageProperties().getMessageId(),
                    retryCount,
                    maxRetries,
                    e
            );

            if (retryCount >= maxRetries) {
                sendToDlq(message, e);
                channel.basicAck(deliveryTag, false);
                return;
            }

            channel.basicReject(deliveryTag, false);
        }
    }

    private int getRetryCountFromMainQueue(Message message) {
        Object rawDeathHeader = message.getMessageProperties().getHeaders().get("x-death");

        if (!(rawDeathHeader instanceof List<?> deaths)) {
            return 0;
        }

        for (Object item : deaths) {
            if (!(item instanceof Map<?, ?> death)) {
                continue;
            }

            Object queue = death.get("queue");
            Object count = death.get("count");

            if (RabbitMqNames.ORDER_CREATED_QUEUE.equals(queue) && count instanceof Number number) {
                return number.intValue();
            }
        }

        return 0;
    }

    private void sendToDlq(Message original, Exception failure) {
        Message failedMessage = MessageBuilder
                .withBody(original.getBody())
                .copyProperties(original.getMessageProperties())
                .setHeader("failure_reason", failure.getMessage())
                .setHeader("failed_at", Instant.now().toString())
                .build();

        rabbitTemplate.send(
                RabbitMqNames.DLX_EXCHANGE,
                RabbitMqNames.ORDER_CREATED_FAILED_ROUTING_KEY,
                failedMessage
        );

        log.error(
                "Message sent to DLQ. messageId={} dlq={}",
                original.getMessageProperties().getMessageId(),
                RabbitMqNames.ORDER_CREATED_DLQ
        );
    }
}
