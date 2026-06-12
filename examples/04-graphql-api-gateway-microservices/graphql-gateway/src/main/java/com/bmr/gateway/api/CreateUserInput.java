package com.bmr.gateway.api;

import com.bmr.gateway.model.UserRole;

public record CreateUserInput(
        String name,
        String email,
        UserRole role
) {
}
