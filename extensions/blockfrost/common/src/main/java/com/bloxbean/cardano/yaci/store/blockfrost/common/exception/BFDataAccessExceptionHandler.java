package com.bloxbean.cardano.yaci.store.blockfrost.common.exception;

import com.bloxbean.cardano.yaci.store.blockfrost.common.timeout.BFQueryTimeoutContext;
import com.bloxbean.cardano.yaci.store.blockfrost.common.timeout.BFQueryTimeouts;
import com.bloxbean.cardano.yaci.store.blockfrost.common.timeout.BFTimeoutResponseAdvice;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Maps database failures (query timeout, lock exhaustion, ...) on Blockfrost endpoints to a Blockfrost-style
 * error response, so a failed query is never served as an empty or zero result. A cancelled (timed out) query
 * answers 504, any other database failure 500.
 */
@Slf4j
@ControllerAdvice(basePackages = "com.bloxbean.cardano.yaci.store.blockfrost")
public class BFDataAccessExceptionHandler {

    static final String MESSAGE = "An unexpected response was received from the backend.";

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<BFErrorResponse> handleDataAccessException(DataAccessException ex,
                                                                     HttpServletRequest request) {
        if (BFQueryTimeouts.isTimeout(ex)) {
            BFQueryTimeoutContext.markTimedOut();
            log.warn("Blockfrost query timed out on {}: {}", request.getRequestURI(), rootCauseMessage(ex));
            return new ResponseEntity<>(BFTimeoutResponseAdvice.timeoutResponse(), HttpStatus.GATEWAY_TIMEOUT);
        }

        log.error("Database error while serving Blockfrost request: {}", ex.getMessage(), ex);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        BFErrorResponse errorResponse = new BFErrorResponse(httpStatus.getReasonPhrase(), MESSAGE, httpStatus.value());
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root)
            root = root.getCause();
        return root.getMessage();
    }
}
