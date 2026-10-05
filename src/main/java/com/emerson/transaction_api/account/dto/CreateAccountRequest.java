package com.emerson.transaction_api.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @JsonProperty("document_number")
        @NotBlank(message = "must not be blank")
        @Size(max = 32, message = "size must be at most 32 characters")
        @Pattern(regexp = "\\d+", message = "must contain digits only")
        String documentNumber) {}
