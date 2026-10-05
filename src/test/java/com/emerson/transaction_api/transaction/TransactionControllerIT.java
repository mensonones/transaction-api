package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Sql(statements = "TRUNCATE TABLE transactions, accounts RESTART IDENTITY",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TransactionControllerIT {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    MockMvc mockMvc;
    long accountId;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        String response = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "document_number": "12345678900" }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        accountId = com.jayway.jsonpath.JsonPath.parse(response).read("$.account_id", Long.class);
    }


    @ParameterizedTest
    @ValueSource(strings = {"0", "null", "0.00001", "1000000000000000"})
    void shouldRejectAmountsOutsideSupportedRange(String amount) throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id\":%d,\"operation_type_id\":4,\"amount\":%s}"
                                .formatted(accountId, amount)))
                .andExpect(status().isBadRequest());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM transactions", Long.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void shouldPersistMaximumAllowedAmount(int operation) throws Exception {
        BigDecimal maximum = new BigDecimal("999999999999999.9999");
        var result = mockMvc.perform(post("/transactions").header("Idempotency-Key", "maximum-amount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id\":%d,\"operation_type_id\":%d,\"amount\":%s}"
                                .formatted(accountId, operation, maximum.toPlainString())))
                .andExpect(status().isCreated()).andReturn();

        BigDecimal expected = operation == 1 ? maximum.negate() : maximum;
        BigDecimal stored = jdbc.queryForObject("SELECT amount FROM transactions WHERE account_id = ?",
                BigDecimal.class, accountId);
        assertEquals(0, expected.compareTo(stored));
        // Parse the exact JSON number as BigDecimal, avoiding double rounding at this limit.
        var json = tools.jackson.databind.json.JsonMapper.builder()
                .enable(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build().readTree(result.getResponse().getContentAsString());
        assertEquals(0, expected.compareTo(new BigDecimal(json.get("amount").asText())));
    }

    @Test
    void shouldCreatePurchaseWithNegativeAmount() throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 1, "amount": 50.00 }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction_id").isNumber())
                .andExpect(jsonPath("$.account_id").value(accountId))
                .andExpect(jsonPath("$.operation_type_id").value(1))
                .andExpect(jsonPath("$.amount").value(-50.00))
                .andExpect(jsonPath("$.event_date").isNotEmpty());
        BigDecimal stored = jdbc.queryForObject("SELECT amount FROM transactions WHERE account_id = ?",
                BigDecimal.class, accountId);
        assertEquals(0, new BigDecimal("-50.00").compareTo(stored));
    }

    @Test
    void shouldCreateInstallmentPurchase() throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 2, "amount": 100.00 }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(-100.00));
    }

    @Test
    void shouldCreateWithdrawal() throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 3, "amount": 25.00 }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(-25.00));
    }

    @Test
    void shouldCreateCreditWithPositiveAmount() throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 4, "amount": 123.45 }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(123.45));
    }

    @Test
    void shouldReturnNotFoundForMissingAccount() throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": 999, "operation_type_id": 1, "amount": 50.00 }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForUnknownOperationType() throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 99, "amount": 50.00 }
                                """.formatted(accountId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectNegativeAmount() throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 1, "amount": -50.00 }
                                """.formatted(accountId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingAccountId() throws Exception {
        mockMvc.perform(post("/transactions").header("Idempotency-Key", "test-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "operation_type_id": 1, "amount": 50.00 }
                                """))
                .andExpect(status().isBadRequest());
    }
}
