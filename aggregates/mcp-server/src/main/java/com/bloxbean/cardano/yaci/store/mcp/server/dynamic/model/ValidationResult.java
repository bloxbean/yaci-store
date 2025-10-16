package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of request validation containing errors if validation failed.
 */
public record ValidationResult(
    boolean valid,
    List<String> errors
) {
    public static ValidationResult success() {
        return new ValidationResult(true, new ArrayList<>());
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public static ValidationResult failure(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorsAsString() {
        return String.join(", ", errors);
    }
}
