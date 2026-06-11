package com.bmr.notifications_worker.messaging;

public final class RabbitMqNames {
    private RabbitMqNames() {
    }

    public static final String ORDERS_EXCHANGE = "orders.events";
    public static final String RETRY_EXCHANGE = "orders.retry";
    public static final String DLX_EXCHANGE = "orders.dlx";

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created.v1";
    public static final String ORDER_CREATED_RETRY_ROUTING_KEY = "order.created.retry.10s";
    public static final String ORDER_CREATED_FAILED_ROUTING_KEY = "order.created.failed";

    public static final String ORDER_CREATED_QUEUE = "orders.created.notifications.q";
    public static final String ORDER_CREATED_RETRY_QUEUE = "orders.created.notifications.retry.10s.q";
    public static final String ORDER_CREATED_DLQ = "orders.created.notifications.dlq";
}
