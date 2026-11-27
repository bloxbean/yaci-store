package com.bloxbean.cardano.yaci.store.analytics.admin;

import com.bloxbean.cardano.yaci.store.analytics.scheduler.PartitionNotReadyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns refused admin requests into a status code with a readable message instead of a 500:
 * 400 for invalid input, 404 for an unknown table or partition, 409 for a partition that is not
 * ready (or an admin export that is already running).
 */
@RestControllerAdvice(assignableTypes = AnalyticsAdminController.class)
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.admin", name = "enabled", havingValue = "true")
public class AnalyticsAdminExceptionHandler {

    /** Error body of a refused admin request. */
    public record ErrorResponse(int status, String error, String message) {
    }

    @ExceptionHandler(AdminApiException.class)
    public ResponseEntity<ErrorResponse> handleAdmin(AdminApiException e) {
        return respond(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(PartitionNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleNotReady(PartitionNotReadyException e) {
        return respond(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid value '" + e.getValue() + "' for " + e.getName());
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
        log.info("Admin request refused ({}): {}", status.value(), message);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message));
    }
}
