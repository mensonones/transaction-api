package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionControllerIT {

    @Autowired
    WebApplicationContext context;

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
                .andReturn().getResponse().getContentAsString();

        accountId = com.jayway.jsonpath.JsonPath.parse(response).read("$.account_id", Long.class);
    }

    @Test
    void deveCriarTransacaoDeCompraComAmountNegativo() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 1, "amount": 50.00 }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction_id").isNumber())
                .andExpect(jsonPath("$.account_id").value(accountId))
                .andExpect(jsonPath("$.operation_type_id").value(1))
                .andExpect(jsonPath("$.amount").value(-50.00));
    }

    @Test
    void deveCriarTransacaoDeCompraParcelada() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 2, "amount": 100.00 }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(-100.00));
    }

    @Test
    void deveCriarTransacaoDeSaque() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 3, "amount": 25.00 }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(-25.00));
    }

    @Test
    void deveCriarTransacaoDeCreditoComAmountPositivo() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 4, "amount": 123.45 }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(123.45));
    }

    @Test
    void deveRetornar404QuandoContaNaoExiste() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": 999, "operation_type_id": 1, "amount": 50.00 }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar404QuandoTipoDeOperacaoEhInvalido() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 99, "amount": 50.00 }
                                """.formatted(accountId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400QuandoAmountEhNegativo() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "account_id": %d, "operation_type_id": 1, "amount": -50.00 }
                                """.formatted(accountId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoAccountIdEhNulo() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "operation_type_id": 1, "amount": 50.00 }
                                """))
                .andExpect(status().isBadRequest());
    }
}
