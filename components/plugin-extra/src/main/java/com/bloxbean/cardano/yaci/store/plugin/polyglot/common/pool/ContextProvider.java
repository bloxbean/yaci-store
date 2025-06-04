package com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.graalvm.polyglot.Context;

public class ContextProvider {
    private GenericKeyedObjectPool<String, Context> genericKeyedObjectPool;

    public ContextProvider(GenericKeyedObjectPool genericKeyedObjectPool) {
        this.genericKeyedObjectPool = genericKeyedObjectPool;
    }

    public GenericKeyedObjectPool<String, Context> getPool() {
        return genericKeyedObjectPool;
    }

}
