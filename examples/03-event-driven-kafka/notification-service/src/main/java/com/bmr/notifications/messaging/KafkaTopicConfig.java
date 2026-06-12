package com.bmr.notifications.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_STATUS_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
