package com.emerson.transaction_api.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @JsonProperty("account_id") @NotNull Long accountId,
        @JsonProperty("operation_type_id") @NotNull Integer operationTypeId,
        @NotNull @Positive BigDecimal amount) {}
