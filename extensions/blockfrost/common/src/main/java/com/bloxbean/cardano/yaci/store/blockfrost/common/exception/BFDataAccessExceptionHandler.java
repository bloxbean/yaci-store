package com.bloxbean.cardano.yaci.store.blockfrost.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Maps database failures (query timeout, lock exhaustion, ...) on Blockfrost endpoints to a Blockfrost-style
 * 500 response, so a failed query is never served as an empty or zero result.
 */
@Slf4j
@ControllerAdvice(basePackages = "com.bloxbean.cardano.yaci.store.blockfrost")
public class BFDataAccessExceptionHandler {

    static final String MESSAGE = "An unexpected response was received from the backend.";

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<BFErrorResponse> handleDataAccessException(DataAccessException ex) {
        log.error("Database error while serving Blockfrost request: {}", ex.getMessage(), ex);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        BFErrorResponse errorResponse = new BFErrorResponse(httpStatus.getReasonPhrase(), MESSAGE, httpStatus.value());
        return new ResponseEntity<>(errorResponse, httpStatus);
    }
}
