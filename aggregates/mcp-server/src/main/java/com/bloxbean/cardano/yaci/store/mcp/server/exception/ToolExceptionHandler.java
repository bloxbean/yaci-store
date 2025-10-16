package com.bloxbean.cardano.yaci.store.mcp.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Global exception handler for MCP tool calls.
 * Logs detailed errors internally while returning user-friendly messages to the AI agent.
 */
@Aspect
@Component
@Slf4j
public class ToolExceptionHandler {

    /**
     * Wraps all @Tool annotated methods to catch and transform exceptions.
     * Full stack traces are logged but only user-friendly messages are returned.
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object handleToolException(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = joinPoint.getSignature().getName();

        try {
            return joinPoint.proceed();
        } catch (NullPointerException e) {
            // Log full details
            log.error("NullPointerException in tool '{}': {}", toolName, e.getMessage(), e);

            // Return user-friendly message
            throw new RuntimeException(
                "Invalid request: Required parameter is missing or null. " +
                "Please provide all required parameters for " + toolName + "."
            );
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException in tool '{}': {}", toolName, e.getMessage(), e);
            throw new RuntimeException(
                "Invalid parameter value for " + toolName + ": " + e.getMessage()
            );
        } catch (RuntimeException e) {
            // If it's already a RuntimeException with a user-friendly message, pass it through
            if (e.getMessage() != null && !e.getMessage().contains("Exception")) {
                log.warn("Tool '{}' error: {}", toolName, e.getMessage());
                throw e;
            }

            // Otherwise, log full details and return generic message
            log.error("Runtime error in tool '{}': {}", toolName, e.getMessage(), e);
            throw new RuntimeException(
                "Error executing " + toolName + ". Please check your parameters and try again."
            );
        } catch (Exception e) {
            // Log full stack trace for unexpected errors
            log.error("Unexpected error in tool '{}': {}", toolName, e.getMessage(), e);
            throw new RuntimeException(
                "Unexpected error executing " + toolName + ". The error has been logged for investigation."
            );
        }
    }
}
