package com.emerson.transaction_api.account;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountControllerIT {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void deveCriarContaComSucesso() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "document_number": "12345678900" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.account_id").isNumber())
                .andExpect(jsonPath("$.document_number").value("12345678900"));
    }

    @Test
    void deveRetornar409QuandoDocumentoJaExiste() throws Exception {
        String body = """
                { "document_number": "12345678900" }
                """;

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar400QuandoDocumentoEhVazio() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "document_number": "" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoDocumentoContemLetras() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "document_number": "abc123" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveBuscarContaPorId() throws Exception {
        String response = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "document_number": "99988877766" }
                                """))
                .andReturn().getResponse().getContentAsString();

        long accountId = com.jayway.jsonpath.JsonPath.parse(response).read("$.account_id", Long.class);

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account_id").value(accountId))
                .andExpect(jsonPath("$.document_number").value("99988877766"));
    }

    @Test
    void deveRetornar404QuandoContaNaoExiste() throws Exception {
        mockMvc.perform(get("/accounts/999"))
                .andExpect(status().isNotFound());
    }
}
