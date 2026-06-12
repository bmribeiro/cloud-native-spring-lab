package com.bmr.users.model;

import java.time.OffsetDateTime;

public record User(
        Long id,
        String name,
        String email,
        UserRole role,
        OffsetDateTime createdAt
) {
}
