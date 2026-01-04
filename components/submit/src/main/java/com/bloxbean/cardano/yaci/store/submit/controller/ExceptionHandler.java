package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.client.function.exception.TxBuildException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Global exception handler for Transaction Lifecycle API.
 * Provides detailed error messages for transaction building failures.
 */
@RestControllerAdvice
@Slf4j
public class ExceptionHandler {

    /**
     * Handle transaction build exceptions with detailed error messages.
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(TxBuildException.class)
    public ResponseEntity<TxErrorResponse> handleTxBuildException(TxBuildException ex) {
        log.error("Transaction build failed: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(TxErrorResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .error("TX_BUILD_ERROR")
                        .message(ex.getMessage())
                        .build());
    }

    /**
     * Handle YAML parsing errors - invalid intent types.
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidTypeIdException.class)
    public ResponseEntity<TxErrorResponse> handleInvalidTypeIdException(InvalidTypeIdException ex) {
        log.error("Invalid YAML structure: {}", ex.getMessage(), ex);

        String message = "Invalid YAML structure: " + ex.getMessage();
        if (ex.getTypeId() != null) {
            message = String.format("Unknown intent type '%s'. Please check the YAML structure and intent type names.",
                                    ex.getTypeId());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(TxErrorResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .error("INVALID_YAML_STRUCTURE")
                        .message(message)
                        .build());
    }

    /**
     * Handle YAML parsing errors - unrecognized properties.
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<TxErrorResponse> handleUnrecognizedPropertyException(UnrecognizedPropertyException ex) {
        log.error("Unrecognized property in YAML: {}", ex.getMessage(), ex);

        String propertyName = ex.getPropertyName();
        String message = String.format("Unrecognized field '%s' in YAML. Known fields are: %s",
                                       propertyName,
                                       ex.getKnownPropertyIds());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(TxErrorResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .error("UNRECOGNIZED_PROPERTY")
                        .message(message)
                        .build());
    }

    /**
     * Handle all other exceptions with generic error message.
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<TxErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error during transaction processing: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TxErrorResponse.builder()
                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("INTERNAL_ERROR")
                        .message("An unexpected error occurred: " + ex.getMessage())
                        .build());
    }
}
