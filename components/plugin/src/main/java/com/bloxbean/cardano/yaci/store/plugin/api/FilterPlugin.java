package com.bloxbean.cardano.yaci.store.plugin.api;

import java.util.Collection;

public interface FilterPlugin<T> extends IPlugin<T> {
    Collection<T> filter(Collection<T> item);
}
