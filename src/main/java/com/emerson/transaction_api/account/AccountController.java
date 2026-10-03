package com.emerson.transaction_api.account;

import com.emerson.transaction_api.account.dto.AccountResponse;
import com.emerson.transaction_api.account.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(account.accountId()).toUri();
        return ResponseEntity.created(location).body(account);
    }

    @GetMapping("/{accountId}")
    public AccountResponse findById(@PathVariable Long accountId) {
        return service.findById(accountId);
    }
}
