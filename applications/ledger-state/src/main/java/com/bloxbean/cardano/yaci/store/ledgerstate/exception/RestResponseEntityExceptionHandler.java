package com.bloxbean.cardano.yaci.store.ledgerstate.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value
            = { UnsupportedOperationException.class })
    protected ResponseEntity<Object> handleUnsupported(
            RuntimeException ex, WebRequest request) {
        String bodyOfResponse = "This api is not implemented yet";
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.NOT_IMPLEMENTED, request);
    }

    @ExceptionHandler(value
            = { IllegalStateException.class })
    protected ResponseEntity<Object> handleIllegalStateException(
            RuntimeException ex, WebRequest request) {
        String bodyOfResponse = "Unexpected error : " + ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
