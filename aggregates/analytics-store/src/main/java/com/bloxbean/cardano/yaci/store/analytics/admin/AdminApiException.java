package com.bloxbean.cardano.yaci.store.analytics.admin;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * An admin request that is refused with a specific HTTP status and message (see
 * {@link AnalyticsAdminExceptionHandler}).
 */
@Getter
public class AdminApiException extends RuntimeException {

    private final HttpStatus status;

    public AdminApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static AdminApiException badRequest(String message) {
        return new AdminApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static AdminApiException notFound(String message) {
        return new AdminApiException(HttpStatus.NOT_FOUND, message);
    }

    public static AdminApiException conflict(String message) {
        return new AdminApiException(HttpStatus.CONFLICT, message);
    }
}
