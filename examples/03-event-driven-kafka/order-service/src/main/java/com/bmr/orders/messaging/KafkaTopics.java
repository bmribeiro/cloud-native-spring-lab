package com.bmr.orders.messaging;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    public static final String ORDER_CREATED = "order.created.v1";
    public static final String PAYMENT_RESULT = "payment.result.v1";
    public static final String ORDER_STATUS_CHANGED = "order.status-changed.v1";
}
