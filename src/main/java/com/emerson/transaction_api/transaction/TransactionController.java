package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.transaction.dto.CreateTransactionRequest;
import com.emerson.transaction_api.transaction.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body, missing Idempotency-Key header, or key with invalid characters"),
            @ApiResponse(responseCode = "404", description = "Account or operation type not found"),
            @ApiResponse(responseCode = "409", description = "Idempotency key already used with different data")
    })
    public TransactionResponse create(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader("Idempotency-Key")
            @Size(min = 1, max = 128, message = "size must be between 1 and 128 characters")
            @Pattern(regexp = "[A-Za-z0-9._:-]+", message = "must contain only letters, digits, dots, underscores, colons or hyphens")
            @Parameter(description = "Unique key (1–128 chars, letters/digits/._:-) to guarantee exactly-once delivery", required = true)
            String idempotencyKey) {
        return transactionService.create(request, idempotencyKey);
    }
}
