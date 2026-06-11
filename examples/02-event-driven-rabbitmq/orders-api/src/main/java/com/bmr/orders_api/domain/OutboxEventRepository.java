package com.bmr.orders_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> findBatchForPublishing(@Param("limit") int limit);
}