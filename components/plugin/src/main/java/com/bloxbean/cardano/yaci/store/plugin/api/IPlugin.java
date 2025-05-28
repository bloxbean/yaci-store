package com.bloxbean.cardano.yaci.store.plugin.api;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;

/**
 * Interface for plugin that can be implemented by various plugin types. event handler, filter, init, post-action, pre-action, etc.
 *
 * @param <T> The type of the plugin.
 */
public interface IPlugin<T> {
    String getName();
    PluginDef getPluginDef();
}
