package com.emerson.transaction_api.shared;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleNotFound(NotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleConflict(ConflictException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(toSnakeCase(error.getField()), error.getDefaultMessage());
        }
        return Map.of("errors", errors);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> handleMissingHeader(MissingRequestHeaderException ex) {
        return Map.of("errors", Map.of(headerToSnakeCase(ex.getHeaderName()), "header is required"));
    }

    // Handles @Pattern / @Size on @RequestHeader/@RequestParam, and @NotNull on @RequestBody record fields (Spring 6+)
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> handleHeaderValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors paramErrors) {
                paramErrors.getFieldErrors().forEach(fe ->
                        errors.putIfAbsent(toSnakeCase(fe.getField()), fe.getDefaultMessage()));
            } else {
                String key = resolveParamKey(result);
                result.getResolvableErrors().forEach(error ->
                        errors.putIfAbsent(key, error.getDefaultMessage()));
            }
        });
        return Map.of("errors", errors);
    }

    private static String resolveParamKey(ParameterValidationResult result) {
        RequestHeader header = result.getMethodParameter().getParameterAnnotation(RequestHeader.class);
        if (header != null) {
            String name = header.value().isEmpty() ? header.name() : header.value();
            if (!name.isEmpty()) return headerToSnakeCase(name);
        }
        return result.getMethodParameter().getParameterName();
    }

    // camelCase field names → snake_case  (e.g. accountId → account_id)
    private static String toSnakeCase(String name) {
        return name.replaceAll("([A-Z])", "_$1").toLowerCase();
    }

    // HTTP header names → snake_case  (e.g. Idempotency-Key → idempotency_key)
    private static String headerToSnakeCase(String name) {
        return name.replace('-', '_').toLowerCase();
    }
}
