package com.bloxbean.cardano.yaci.store.plugin.api;

import java.util.Collection;

public interface PreActionPlugin<T> extends IPlugin<T> {
    void preAction(Collection<T> item);
}
