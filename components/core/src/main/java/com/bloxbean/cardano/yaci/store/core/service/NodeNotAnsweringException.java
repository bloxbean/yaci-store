package com.bloxbean.cardano.yaci.store.core.service;

/**
 * The node accepted the connection but did not answer in time. This can pass on its own, so the start is retried.
 */
public class NodeNotAnsweringException extends IllegalStateException {
    public NodeNotAnsweringException(String message) {
        super(message);
    }
}
