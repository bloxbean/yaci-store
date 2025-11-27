package com.bloxbean.cardano.yaci.store.blockfrost.epoch.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class BFGlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<BFErrorResponse> handleResponseStatusException(ResponseStatusException ex) {

        int statusCode = ex.getStatusCode().value();
        String message = ex.getReason();

        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
        String error = httpStatus.getReasonPhrase();

        BFErrorResponse errorResponse = new BFErrorResponse(error, message, statusCode);
        return new ResponseEntity<>(errorResponse, httpStatus);

    }
}
