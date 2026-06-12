package com.bmr.orders.repository;


import com.bmr.orders.model.Order;
import com.bmr.orders.model.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Order> rowMapper = (rs, rowNum) -> new Order(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getBigDecimal("total"),
            OrderStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", java.time.OffsetDateTime.class)
    );

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Order> findAll() {
        return jdbcTemplate.query(
                "SELECT id, user_id, total, status, created_at FROM orders ORDER BY id",
                rowMapper
        );
    }

    public List<Order> findByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, total, status, created_at FROM orders WHERE user_id = ? ORDER BY id",
                rowMapper,
                userId
        );
    }

    public Optional<Order> findById(Long id) {
        return jdbcTemplate.query(
                        "SELECT id, user_id, total, status, created_at FROM orders WHERE id = ?",
                        rowMapper,
                        id
                )
                .stream()
                .findFirst();
    }

    public Order create(Long userId, java.math.BigDecimal total, OrderStatus status) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO orders (user_id, total, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                total,
                status.name()
        );

        if (id == null) {
            throw new IllegalStateException("Database did not return a generated order id");
        }

        return findById(id)
                .orElseThrow(() -> new IllegalStateException("Created order was not found"));
    }

    public Optional<Order> updateStatus(Long id, OrderStatus status) {
        int updated = jdbcTemplate.update(
                "UPDATE orders SET status = ? WHERE id = ?",
                status.name(),
                id
        );

        if (updated == 0) {
            return Optional.empty();
        }

        return findById(id);
    }
}
