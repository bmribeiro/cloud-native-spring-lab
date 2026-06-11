package com.bmr.notifications_worker.service;

import com.bmr.notifications_worker.domain.NotificationLogEntity;
import com.bmr.notifications_worker.domain.NotificationLogRepository;
import com.bmr.notifications_worker.domain.ProcessedMessageEntity;
import com.bmr.notifications_worker.domain.ProcessedMessageRepository;
import com.bmr.notifications_worker.events.OrderCreatedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ProcessedMessageRepository processedMessageRepository;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(ProcessedMessageRepository processedMessageRepository,
                               NotificationLogRepository notificationLogRepository) {
        this.processedMessageRepository = processedMessageRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Transactional
    public void handleOrderCreated(UUID eventId, OrderCreatedPayload payload) {
        if (processedMessageRepository.existsById(eventId)) {
            log.info("Event already processed. eventId={}", eventId);
            return;
        }

        if (payload.customerEmail().endsWith("@fail.local")) {
            throw new IllegalStateException("Forced failure for retry/DLQ test");
        }

        NotificationLogEntity logEntry = new NotificationLogEntity(
                payload.orderId(),
                eventId,
                payload.customerEmail(),
                "SENT_SIMULATED",
                "Order confirmation notification generated"
        );

        notificationLogRepository.save(logEntry);
        processedMessageRepository.save(new ProcessedMessageEntity(eventId));
    }
}