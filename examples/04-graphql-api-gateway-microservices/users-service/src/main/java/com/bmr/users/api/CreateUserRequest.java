package com.bmr.users.api;

import com.bmr.users.model.UserRole;

public record CreateUserRequest(
        String name,
        String email,
        UserRole role
) {
}
