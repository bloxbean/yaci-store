package com.bloxbean.cardano.yaci.store.submit.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TxPlanBuildException extends RuntimeException {
    public TxPlanBuildException(String message) {
        super(message);
    }

    public TxPlanBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
