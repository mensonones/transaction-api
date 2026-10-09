package com.emerson.transaction_api.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(
        @JsonProperty("transaction_id") Long transactionId,
        @JsonProperty("account_id") Long accountId,
        @JsonProperty("operation_type_id") Integer operationTypeId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("balance")  BigDecimal balance,
        @JsonProperty("event_date") OffsetDateTime eventDate) {}
