package com.emerson.transaction_api.account;

import com.emerson.transaction_api.account.dto.AccountResponse;
import com.emerson.transaction_api.account.dto.CreateAccountRequest;
import com.emerson.transaction_api.shared.ConflictException;
import com.emerson.transaction_api.shared.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public AccountResponse create(CreateAccountRequest request) {
        String document = request.documentNumber().trim();
        if (accounts.existsByDocumentNumber(document)) throw new ConflictException("Document already exists");
        Account saved = accounts.save(new Account(document));
        return toResponse(saved);
    }

    public AccountResponse findById(Long id) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return toResponse(account);
    }

    private static AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getDocumentNumber());
    }
}
