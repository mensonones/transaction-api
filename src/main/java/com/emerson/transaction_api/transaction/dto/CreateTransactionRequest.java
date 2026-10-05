package com.emerson.transaction_api.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @JsonProperty("account_id") @NotNull(message = "must not be null") Long accountId,
        @JsonProperty("operation_type_id") @NotNull(message = "must not be null") Integer operationTypeId,
        @NotNull(message = "must not be null")
        @Positive(message = "must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "numeric value out of bounds (15 integer digits, 4 fraction digits expected)")
        BigDecimal amount) {}
