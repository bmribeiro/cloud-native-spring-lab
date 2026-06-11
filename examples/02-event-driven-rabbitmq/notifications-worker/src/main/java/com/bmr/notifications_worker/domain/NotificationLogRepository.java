package com.bmr.notifications_worker.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, UUID> {
}
