package com.bmr.notifications_worker.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqTopologyConfig {
    @Bean
    DirectExchange ordersExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqNames.ORDERS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    DirectExchange retryExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqNames.RETRY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    DirectExchange dlxExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqNames.DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    Queue orderCreatedQueue() {
        return QueueBuilder
                .durable(RabbitMqNames.ORDER_CREATED_QUEUE)
                .deadLetterExchange(RabbitMqNames.RETRY_EXCHANGE)
                .deadLetterRoutingKey(RabbitMqNames.ORDER_CREATED_RETRY_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue orderCreatedRetryQueue() {
        return QueueBuilder
                .durable(RabbitMqNames.ORDER_CREATED_RETRY_QUEUE)
                .ttl(10_000)
                .deadLetterExchange(RabbitMqNames.ORDERS_EXCHANGE)
                .deadLetterRoutingKey(RabbitMqNames.ORDER_CREATED_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue orderCreatedDlq() {
        return QueueBuilder
                .durable(RabbitMqNames.ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, DirectExchange ordersExchange) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(ordersExchange)
                .with(RabbitMqNames.ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    Binding orderCreatedRetryBinding(Queue orderCreatedRetryQueue, DirectExchange retryExchange) {
        return BindingBuilder
                .bind(orderCreatedRetryQueue)
                .to(retryExchange)
                .with(RabbitMqNames.ORDER_CREATED_RETRY_ROUTING_KEY);
    }

    @Bean
    Binding orderCreatedDlqBinding(Queue orderCreatedDlq, DirectExchange dlxExchange) {
        return BindingBuilder
                .bind(orderCreatedDlq)
                .to(dlxExchange)
                .with(RabbitMqNames.ORDER_CREATED_FAILED_ROUTING_KEY);
    }
}
