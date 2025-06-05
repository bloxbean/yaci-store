package com.bloxbean.cardano.yaci.store.plugin.api;

import java.util.Collection;

/**
 * Interface for pre-action plugins that can perform actions before target method invocation and after filter plugin execution.
 *
 * @param <T> The type of the plugin.
 */
public interface PreActionPlugin<T> extends IPlugin<T> {
    Collection<T> preAction(Collection<T> item);
}
