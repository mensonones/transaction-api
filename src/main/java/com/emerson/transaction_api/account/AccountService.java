package com.emerson.transaction_api.account;

import com.emerson.transaction_api.account.dto.AccountResponse;
import com.emerson.transaction_api.account.dto.CreateAccountRequest;
import com.emerson.transaction_api.shared.ConflictException;
import com.emerson.transaction_api.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

@Service
public class AccountService {
    private static final String UNIQUE_VIOLATION = "23505";

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accounts) {
        this.accountRepository = accounts;
    }

    public AccountResponse create(CreateAccountRequest request) {
        String document = request.documentNumber().trim();
        try {
            Account saved = accountRepository.saveAndFlush(new Account(document));
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // PostgreSQL reports a duplicate unique value with SQLState 23505.
            if (ex.getMostSpecificCause() instanceof SQLException sql
                    && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                throw new ConflictException("Document already exists");
            }
            throw ex;
        }
    }

    public AccountResponse findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return toResponse(account);
    }

    private static AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getAccountId(), account.getDocumentNumber());
    }
}
