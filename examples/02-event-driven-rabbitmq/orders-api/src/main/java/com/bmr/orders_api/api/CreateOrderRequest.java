package com.bmr.orders_api.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank @Email String customerEmail,
        @Positive int amountCents,
        @NotBlank String currency
) {
}
