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
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request, String idempotencyKey) {
        Account account = accountRepository.findByAccountId(request.accountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        OperationType operationType = OperationType.fromId(request.operationTypeId());

        BigDecimal amount = operationType.isDebit()
                ? request.amount().negate()
                : request.amount();

        int inserted = transactionRepository.insertIfKeyIsNew(account.getAccountId(), operationType.getId(), amount,
                OffsetDateTime.now(), idempotencyKey);
        Transaction transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Transaction missing after insert for key: " + idempotencyKey));
        if (!transaction.getAccount().getAccountId().equals(request.accountId())
                || !transaction.getOperationTypeId().equals(request.operationTypeId())
                || transaction.getAmount().compareTo(amount) != 0) {
            throw new ConflictException("Idempotency key already used with different transaction data");
        }

        if (inserted == 1 && operationType == OperationType.CREDIT_VOUCHER) {
            discharge(transaction);
        }

        return toResponse(transaction);
    }

    private void discharge(Transaction transaction) {
        BigDecimal transactionBalance = transaction.getBalance();
        List<Transaction> debits = transactionRepository
                .findByAccount_AccountIdAndBalanceLessThanOrderByEventDateAscTransactionId(transaction.getAccount().getAccountId(), BigDecimal.ZERO);

        for (Transaction debit : debits) {
            if (transactionBalance.signum() <= 0) break;

            BigDecimal debitBalance = debit.getBalance();
            BigDecimal debitAmount = transactionBalance.min(debitBalance.negate());
            debit.updateBalance(debitBalance.add(debitAmount));

            transactionBalance  = transactionBalance.subtract(debitAmount);
        }

        transaction.updateBalance(transactionBalance);
    }

    private static TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getTransactionId(),
                t.getAccount().getAccountId(),
                t.getOperationTypeId(),
                t.getAmount(),
                t.getBalance(),
                t.getEventDate());
    }
}
