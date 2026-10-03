package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.account.Account;
import com.emerson.transaction_api.account.AccountRepository;
import com.emerson.transaction_api.operation.OperationType;
import com.emerson.transaction_api.shared.NotFoundException;
import com.emerson.transaction_api.transaction.dto.CreateTransactionRequest;
import com.emerson.transaction_api.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionService {
    private final TransactonRepository transactions;
    private final AccountRepository accounts;

    public TransactionService(TransactonRepository transactions, AccountRepository accounts) {
        this.transactions = transactions;
        this.accounts = accounts;
    }

    public TransactionResponse create(CreateTransactionRequest request) {
        Account account = accounts.findById(request.accountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        OperationType operationType = OperationType.fromId(request.operationTypeId());

        BigDecimal amount = operationType.isDebit()
                ? request.amount().negate()
                : request.amount();

        Transaction saved = transactions.save(new Transaction(account, request.operationTypeId(), amount));
        return toResponse(saved);
    }

    private static TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAccount().getId(),
                t.getOperationTypeId(),
                t.getAmount(),
                t.getEventDate());
    }
}
