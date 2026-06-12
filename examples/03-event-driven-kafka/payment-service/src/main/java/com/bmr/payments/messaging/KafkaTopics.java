package com.bmr.payments.messaging;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    public static final String ORDER_CREATED = "order.created.v1";
    public static final String PAYMENT_RESULT = "payment.result.v1";
}
