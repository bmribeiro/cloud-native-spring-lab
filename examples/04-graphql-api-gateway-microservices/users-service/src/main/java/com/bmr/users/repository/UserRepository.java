package com.bmr.users.repository;

import com.bmr.users.model.UserRole;
import com.bmr.users.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> rowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            UserRole.valueOf(rs.getString("role")),
            rs.getObject("created_at", java.time.OffsetDateTime.class)
    );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> findAll() {
        return jdbcTemplate.query(
                "SELECT id, name, email, role, created_at FROM users ORDER BY id",
                rowMapper
        );
    }

    public Optional<User> findById(Long id) {
        return jdbcTemplate.query(
                        "SELECT id, name, email, role, created_at FROM users WHERE id = ?",
                        rowMapper,
                        id
                )
                .stream()
                .findFirst();
    }

    public List<User> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id, name, email, role, created_at FROM users WHERE id IN (" + placeholders + ") ORDER BY id";

        return jdbcTemplate.query(sql, rowMapper, ids.toArray());
    }

    public User create(String name, String email, UserRole role) throws DuplicateKeyException {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO users (name, email, role) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                name,
                email,
                role.name()
        );

        if (id == null) {
            throw new IllegalStateException("Database did not return a generated user id");
        }

        return findById(id)
                .orElseThrow(() -> new IllegalStateException("Created user was not found"));
    }
}
