package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.account.Account;
import com.emerson.transaction_api.account.AccountRepository;
import com.emerson.transaction_api.operation.OperationType;
import com.emerson.transaction_api.shared.NotFoundException;
import com.emerson.transaction_api.shared.ConflictException;
import com.emerson.transaction_api.transaction.dto.CreateTransactionRequest;
import com.emerson.transaction_api.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final AccountRepository accounts;

    public TransactionService(TransactionRepository transactions, AccountRepository accounts) {
        this.transactions = transactions;
        this.accounts = accounts;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request, String idempotencyKey) {
        Account account = accounts.findById(request.accountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        OperationType operationType = OperationType.fromId(request.operationTypeId());

        BigDecimal amount = operationType.isDebit()
                ? request.amount().negate()
                : request.amount();

        transactions.insertIfKeyIsNew(account.getAccountId(), operationType.getId(), amount,
                OffsetDateTime.now(), idempotencyKey);
        Transaction transaction = transactions.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Transaction missing after insert for key: " + idempotencyKey));
        if (!transaction.getAccount().getAccountId().equals(request.accountId())
                || !transaction.getOperationTypeId().equals(request.operationTypeId())
                || transaction.getAmount().compareTo(amount) != 0) {
            throw new ConflictException("Idempotency key already used with different transaction data");
        }
        return toResponse(transaction);
    }

    private static TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getTransactionId(),
                t.getAccount().getAccountId(),
                t.getOperationTypeId(),
                t.getAmount(),
                t.getEventDate());
    }
}
