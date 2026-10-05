package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Sql(statements = "TRUNCATE TABLE transactions, accounts RESTART IDENTITY",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TransactionIdempotencyIT {
    @Autowired
    WebApplicationContext context;
    @Autowired
    JdbcTemplate jdbc;
    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbc.update("INSERT INTO accounts (document_number) VALUES ('12345678900'), ('98765432100')");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldReturnOriginalTransactionOnRetry(int operation) throws Exception {
        var first = create("payment-1", 1, operation, "12.34");
        var retry = create("payment-1", 1, operation, "12.3400");
        assertEquals(201, first.getStatus());
        assertEquals(201, retry.getStatus());
        assertEquals(first.getContentAsString(), retry.getContentAsString());
        assertEquals(1, count());
    }

    @ParameterizedTest
    @CsvSource({"2, 4, 12.34", "1, 1, 12.34", "1, 4, 99.00"})
    void shouldRejectKeyReusedWithDifferentData(long account, int operation, String amount) throws Exception {
        var original = create("payment-1", 1, 4, "12.34");
        assertEquals(201, original.getStatus());
        assertEquals(409, create("payment-1", account, operation, amount).getStatus());
        assertEquals(original.getContentAsString(), create("payment-1", 1, 4, "12.34").getContentAsString());
        assertEquals(1, count());
    }

    @Test
    void shouldCreateIndependentTransactionsWithDifferentKeys() throws Exception {
        assertEquals(201, create("payment-1", 1, 4, "12.34").getStatus());
        assertEquals(201, create("payment-2", 1, 4, "12.34").getStatus());
        assertEquals(2, count());
    }

    @Test
    void shouldRejectRequestsWithoutKey() throws Exception {
        assertEquals(400, create(null, 1, 4, "12.34").getStatus());
        assertEquals(0, count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "key with spaces"})
    void shouldRejectInvalidKeys(String key) throws Exception {
        assertEquals(400, create(key, 1, 4, "12.34").getStatus());
        assertEquals(0, count());
    }

    @Test
    void shouldEnforceKeyLengthLimit() throws Exception {
        assertEquals(400, create("a".repeat(129), 1, 4, "12.34").getStatus());
        assertEquals(201, create("a".repeat(128), 1, 4, "12.34").getStatus());
        assertEquals(1, count());
    }

    @Test
    void shouldNotReserveKeyForFailedRequests() throws Exception {
        assertEquals(400, create("payment-1", 1, 4, "-1").getStatus());
        assertEquals(404, create("payment-1", 99, 4, "12.34").getStatus());
        assertEquals(404, create("payment-1", 1, 99, "12.34").getStatus());
        assertEquals(0, count());
        assertEquals(201, create("payment-1", 1, 4, "12.34").getStatus());
        assertEquals(1, count());
    }

    @Test
    void shouldPreventDuplicateKeysDirectlyInDatabase() throws Exception {
        assertEquals(201, create("payment-1", 1, 4, "12.34").getStatus());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("""
                        INSERT INTO transactions (account_id, operation_type_id, amount, event_date, idempotency_key)
                        VALUES (1, 4, 12.34, now(), 'payment-1')
                        """));
        assertEquals(1, count());
    }

    @Test
    void shouldReturnSameTransactionForConcurrentRetries() throws Exception {
        // Two separate HTTP requests compete for the same database key.
        var start = new CyclicBarrier(2);
        Callable<MockHttpServletResponse> request = () -> {
            start.await(10, TimeUnit.SECONDS);
            return create("payment-1", 1, 4, "12.34");
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(request, request), 20, TimeUnit.SECONDS);
            var first = results.get(0).get();
            var second = results.get(1).get();
            assertEquals(201, first.getStatus());
            assertEquals(201, second.getStatus());
            assertEquals(first.getContentAsString(), second.getContentAsString());
            assertEquals(1, count());
        }
    }

    @Test
    void shouldRejectDifferentDataCompetingForSameKey() throws Exception {
        var start = new CyclicBarrier(2);
        Callable<MockHttpServletResponse> firstRequest = () -> {
            start.await(10, TimeUnit.SECONDS);
            return create("payment-1", 1, 4, "12.34");
        };
        Callable<MockHttpServletResponse> secondRequest = () -> {
            start.await(10, TimeUnit.SECONDS);
            return create("payment-1", 1, 4, "99.00");
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(firstRequest, secondRequest), 20, TimeUnit.SECONDS);
            var first = results.get(0).get();
            var second = results.get(1).get();
            assertEquals(List.of(201, 409),
                    java.util.stream.Stream.of(first.getStatus(), second.getStatus()).sorted().toList());
            assertEquals(1, count());
            String winningAmount = first.getStatus() == 201 ? "12.34" : "99.00";
            var stored = jdbc.queryForObject("SELECT amount FROM transactions WHERE idempotency_key = 'payment-1'",
                    java.math.BigDecimal.class);
            assertEquals(0, new java.math.BigDecimal(winningAmount).compareTo(stored));
            var winner = first.getStatus() == 201 ? first : second;
            assertEquals(winner.getContentAsString(),
                    create("payment-1", 1, 4, winningAmount).getContentAsString());
        }
    }

    private MockHttpServletResponse create(String key, long account, int operation, String amount) throws Exception {
        var request = post("/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account_id\":%d,\"operation_type_id\":%d,\"amount\":%s}"
                        .formatted(account, operation, amount));
        if (key != null) request.header("Idempotency-Key", key);
        return mockMvc.perform(request).andReturn().getResponse();
    }

    private int count() {
        return jdbc.queryForObject("SELECT count(*) FROM transactions", Integer.class);
    }
}
