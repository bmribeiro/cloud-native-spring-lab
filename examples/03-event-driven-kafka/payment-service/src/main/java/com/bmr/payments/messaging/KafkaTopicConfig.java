package com.bmr.payments.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic orderCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic paymentResultTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_RESULT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic orderCreatedDeadLetterTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
