package com.bloxbean.cardano.yaci.store.plugin.api;

import java.util.Collection;

/**
 * Interface for post-action plugins that can perform actions after target method invocation completes.
 *
 * @param <T> The type of the plugin.
 */
public interface PostActionPlugin<T> extends IPlugin<T> {
    void postAction(Collection<T> item);
}
