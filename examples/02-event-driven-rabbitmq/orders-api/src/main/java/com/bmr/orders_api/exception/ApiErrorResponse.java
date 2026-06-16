package com.bmr.orders_api.exception;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp
) {
}