package com.bloxbean.cardano.yaci.store.snapshot.spec;

/** Raised when a specification is syntactically or semantically invalid, or the registry conflicts. */
public class SpecException extends RuntimeException {
    public SpecException(String message) {
        super(message);
    }

    public SpecException(String message, Throwable cause) {
        super(message, cause);
    }
}
