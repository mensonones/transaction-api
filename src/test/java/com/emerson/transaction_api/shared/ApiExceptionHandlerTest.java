package com.emerson.transaction_api.shared;

import com.emerson.transaction_api.account.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void shouldReturnFieldErrorsForInvalidInput() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document_number\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.document_number").isNotEmpty());
    }

    @Test
    void shouldPreserveConflictStatus() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Document already exists"));
    }

    @Test
    void shouldPreserveNotFoundStatus() throws Exception {
        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPreserveBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPreserveBadRequestForInvalidParameter() throws Exception {
        mockMvc.perform(get("/test/number").param("value", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPreserveMethodNotAllowedStatus() throws Exception {
        mockMvc.perform(post("/test/missing"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldReturnBadRequestForMissingHeader() throws Exception {
        mockMvc.perform(get("/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.x_key").value("header is required"));
    }

    @Test
    void shouldReturnBadRequestForInvalidHeader() throws Exception {
        mockMvc.perform(get("/test/header").header("X-Key", "invalid value with spaces"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.x_key").isNotEmpty());
    }

    @RestController
    static class TestController {
        @PostMapping("/test/validation")
        CreateAccountRequest validation(@Valid @RequestBody CreateAccountRequest body) { return body; }

        @GetMapping("/test/conflict")
        String conflict() { throw new ConflictException("Document already exists"); }

        @GetMapping("/test/missing")
        String missing() { throw new NotFoundException("Account not found"); }

        @PostMapping("/test/body")
        Map<String, Object> body(@RequestBody Map<String, Object> body) { return body; }

        @GetMapping("/test/number")
        int number(@RequestParam int value) { return value; }

        @GetMapping("/test/header")
        String header(@RequestHeader("X-Key") @Pattern(regexp = "[A-Za-z0-9]+") String key) { return key; }
    }
}
