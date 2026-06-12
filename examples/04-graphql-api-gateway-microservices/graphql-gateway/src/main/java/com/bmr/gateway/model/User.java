package com.bmr.gateway.model;

import com.bmr.gateway.model.UserRole;

import java.time.OffsetDateTime;

public record User(
        Long id,
        String name,
        String email,
        UserRole role,
        OffsetDateTime createdAt
) {
}
