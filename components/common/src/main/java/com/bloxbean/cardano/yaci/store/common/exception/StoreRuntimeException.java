package com.bloxbean.cardano.yaci.store.common.exception;

public class StoreRuntimeException extends RuntimeException {
    public StoreRuntimeException(String message) {
        super(message);
    }

    public StoreRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
