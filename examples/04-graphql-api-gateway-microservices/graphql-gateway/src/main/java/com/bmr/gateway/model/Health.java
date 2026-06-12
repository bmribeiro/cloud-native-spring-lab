package com.bmr.gateway.model;

public record Health(
        String gateway,
        String usersService,
        String ordersService
) {
}
