package com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.graalvm.polyglot.Context;

/**
 * Provides a pool of Polyglot Contexts.
 * This class is used to manage and provide access to a pool of Polyglot Contexts.
 * It uses a GenericKeyedObjectPool to manage the contexts keyed by a String identifier.
 */
public class ContextProvider {
    private GenericKeyedObjectPool<String, Context> genericKeyedObjectPool;

    public ContextProvider(GenericKeyedObjectPool genericKeyedObjectPool) {
        this.genericKeyedObjectPool = genericKeyedObjectPool;
    }

    public GenericKeyedObjectPool<String, Context> getPool() {
        return genericKeyedObjectPool;
    }

}
