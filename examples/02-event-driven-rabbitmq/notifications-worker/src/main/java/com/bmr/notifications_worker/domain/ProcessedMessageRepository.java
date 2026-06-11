package com.bmr.notifications_worker.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessageEntity, UUID> {
}
