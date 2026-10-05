package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.account.Account;
import com.emerson.transaction_api.account.AccountRepository;
import com.emerson.transaction_api.transaction.dto.CreateTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    TransactionRepository transactions;

    @Mock
    AccountRepository accounts;

    TransactionService service;

    @BeforeEach
    void setup() {
        service = new TransactionService(transactions, accounts);
    }

    @ParameterizedTest(name = "{0}: operation {1} should save amount {2}")
    @CsvSource({
            "Purchase, 1, -123.45",
            "Purchase with installments, 2, -123.45",
            "Withdrawal, 3, -123.45",
            "Credit voucher, 4, 123.45"
    })
    void shouldApplyAmountSignAccordingToOperation(String description, int operationTypeId,
                                               BigDecimal expectedAmount) {
        Account account = new Account("12345678900");
        account.setAccountId(1L);
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        when(transactions.findByIdempotencyKey("payment-1")).thenReturn(Optional.of(
                new Transaction(account, operationTypeId, expectedAmount)));

        var response = service.create(new CreateTransactionRequest(
                1L, operationTypeId, new BigDecimal("123.45")), "payment-1");

        var captured = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transactions).insertIfKeyIsNew(eq(1L), eq(operationTypeId), captured.capture(),
                any(OffsetDateTime.class), eq("payment-1"));
        assertEquals(0, expectedAmount.compareTo(captured.getValue()),
                "The amount sent to the repository should have the correct sign and magnitude");
        assertEquals(0, expectedAmount.compareTo(response.amount()),
                "The response should contain the amount with the correct sign");
    }
}
