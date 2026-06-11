package com.bmr.notifications_worker.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTemplateConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitTemplateConfig.class);

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                String id = correlationData == null ? "unknown" : correlationData.getId();
                log.error("RabbitMQ publish was not confirmed. correlationId={}, cause={}", id, cause);
            }
        });

        template.setReturnsCallback(returned -> log.error(
                "RabbitMQ returned unroutable message. exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        ));

        return template;
    }
}
