package com.bloxbean.cardano.yaci.store.plugin.api;

import java.util.Collection;

/**
 * Interface for filter plugins that can filter a collection of items.
 *
 * @param <T>
 */
public interface FilterPlugin<T> extends IPlugin<T> {
    Collection<T> filter(Collection<T> item);
}
