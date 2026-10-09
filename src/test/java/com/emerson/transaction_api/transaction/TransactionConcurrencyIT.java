package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.TestcontainersConfiguration;
import com.emerson.transaction_api.transaction.dto.CreateTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Sql(statements = "TRUNCATE TABLE transactions, accounts RESTART IDENTITY",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TransactionConcurrencyIT {

    @Autowired
    TransactionService service;

    @Autowired
    JdbcTemplate jdbc;

    long accountId;

    @BeforeEach
    void setup() {
        jdbc.update("INSERT INTO accounts (document_number) VALUES ('12345678900')");
        accountId = jdbc.queryForObject("SELECT account_id FROM accounts WHERE document_number = '12345678900'", Long.class);
        // debit of -100
        service.create(new CreateTransactionRequest(accountId, 1, new BigDecimal("100.00")), "debit-1");
    }

    @Test
    void twoSimultaneousCreditsShouldNotDoubleDischarge() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            latch.await();
            service.create(new CreateTransactionRequest(accountId, 4, new BigDecimal("60.00")), "credit-1");
            return null;
        });
        executor.submit(() -> {
            latch.await();
            service.create(new CreateTransactionRequest(accountId, 4, new BigDecimal("60.00")), "credit-2");
            return null;
        });

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        BigDecimal debitBalance = jdbc.queryForObject(
                "SELECT balance FROM transactions WHERE idempotency_key = 'debit-1'", BigDecimal.class);
        BigDecimal totalCreditBalance = jdbc.queryForObject(
                "SELECT SUM(balance) FROM transactions WHERE operation_type_id = 4", BigDecimal.class);

        assertEquals(0, BigDecimal.ZERO.compareTo(debitBalance),
                "Debit should be fully discharged: " + debitBalance);
        assertEquals(0, new BigDecimal("20.00").compareTo(totalCreditBalance),
                "Total credit balance should be 20.00: " + totalCreditBalance);
    }
}
