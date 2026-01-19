package com.bloxbean.cardano.yaci.store.submit.quicktx.signing.remote;

public class RemoteSignerException extends RuntimeException {
    public RemoteSignerException(String message) {
        super(message);
    }

    public RemoteSignerException(String message, Throwable cause) {
        super(message, cause);
    }
}
