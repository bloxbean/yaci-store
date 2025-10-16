package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.validation;

import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.DynamicQueryRequest;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.FilterCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates dynamic query requests for security vulnerabilities.
 * Prevents SQL injection and other security issues.
 */
@Component
@Slf4j
public class SecurityValidator {

    // Pattern to detect SQL injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(;|--|/\\*|\\*/|xp_|sp_|exec|execute|select|insert|update|delete|drop|create|alter|union|declare|cast|convert)",
        Pattern.CASE_INSENSITIVE
    );

    // Maximum string length for values
    private static final int MAX_STRING_LENGTH = 1000;

    // Maximum array size for IN/BETWEEN operators
    private static final int MAX_ARRAY_SIZE = 100;

    /**
     * Validates security aspects of the request.
     * Throws SecurityException if security violation is detected.
     */
    public void validateSecurity(DynamicQueryRequest request) {
        // Validate table name
        validateIdentifier(request.table(), "table");

        // Validate all filter values
        validateFilterValues(request.filters());
        validateFilterValues(request.having());
    }

    private void validateIdentifier(String identifier, String type) {
        if (identifier == null) {
            return;
        }

        // Identifiers must be alphanumeric with underscores
        if (!identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new SecurityException("Invalid " + type + " name format: " + identifier);
        }

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(identifier).find()) {
            log.warn("SQL injection attempt detected in {}: {}", type, identifier);
            throw new SecurityException("Suspicious pattern detected in " + type);
        }
    }

    private void validateFilterValues(List<FilterCondition> filters) {
        for (FilterCondition filter : filters) {
            validateFilterValue(filter.value());
        }
    }

    private void validateFilterValue(Object value) {
        if (value == null) {
            return;
        }

        // Handle string values
        if (value instanceof String str) {
            validateStringValue(str);
        }
        // Handle list/array values (for IN, BETWEEN)
        else if (value instanceof List<?> list) {
            if (list.size() > MAX_ARRAY_SIZE) {
                throw new SecurityException("Array size exceeds maximum: " + list.size());
            }
            for (Object item : list) {
                validateFilterValue(item);
            }
        }
        // Handle numeric values
        else if (value instanceof Number) {
            // Numbers are safe, no validation needed
        }
        // Handle boolean values
        else if (value instanceof Boolean) {
            // Booleans are safe, no validation needed
        }
        // Reject unknown types
        else {
            throw new SecurityException("Unsupported value type: " + value.getClass().getName());
        }
    }

    private void validateStringValue(String value) {
        // Check length
        if (value.length() > MAX_STRING_LENGTH) {
            throw new SecurityException("String value exceeds maximum length: " + value.length());
        }

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(value).find()) {
            log.warn("SQL injection attempt detected in value: {}", value);
            throw new SecurityException("Suspicious pattern detected in filter value");
        }

        // Check for null bytes (can bypass security in some databases)
        if (value.contains("\0")) {
            throw new SecurityException("Null byte detected in string value");
        }
    }
}
