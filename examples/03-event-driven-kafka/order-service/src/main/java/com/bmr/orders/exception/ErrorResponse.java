package com.bmr.orders.exception;

import java.time.Instant;

public record ErrorResponse(
        String message,
        String errorCode,
        Instant timestamp
) {
    public ErrorResponse(String message) {
        this(message, "INTERNAL_ERROR", Instant.now());
    }

    public ErrorResponse(String message, String errorCode) {
        this(message, errorCode, Instant.now());
    }
}