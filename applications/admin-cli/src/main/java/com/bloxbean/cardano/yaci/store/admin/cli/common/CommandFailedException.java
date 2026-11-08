package com.bloxbean.cardano.yaci.store.admin.cli.common;

/**
 * Thrown by a command after it has already printed why it failed, so the process exits non-zero
 * without a second message. Scripts and automation depend on the exit code.
 */
public class CommandFailedException extends RuntimeException {

    public static final int EXIT_CODE = 1;

    public CommandFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
