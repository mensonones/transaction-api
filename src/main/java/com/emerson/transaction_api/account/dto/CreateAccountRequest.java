package com.emerson.transaction_api.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @JsonProperty("document_number")
        @NotBlank @Size(max = 32) @Pattern(regexp = "\\d+", message = "must contain digits only") String documentNumber) {}
