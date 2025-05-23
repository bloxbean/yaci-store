package com.bloxbean.cardano.yaci.store.plugin.api;

import java.util.Collection;

public interface PostActionPlugin<T> extends IPlugin<T> {
    void postAction(Collection<T> item);
}
