package com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool;

import org.graalvm.polyglot.Context;

/**
 * Interface for providing a Polyglot Context.
 * Implement this interface to create and provide a Polyglot Context.
 * This is typically used in conjunction with a pool of contexts.
 */
public interface ContextSupplier {
    Context createContext();
}
