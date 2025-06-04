package com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool;

import org.graalvm.polyglot.Context;

public interface ContextSupplier {
    Context createContext();
}
