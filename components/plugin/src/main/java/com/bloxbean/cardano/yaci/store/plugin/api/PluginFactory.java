package com.bloxbean.cardano.yaci.store.plugin.api;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;

public interface PluginFactory {
    String getType();

    <T> InitPlugin createInitPlugin(PluginDef def);

    <T> FilterPlugin<T> createFilterPlugin(PluginDef def);

    <T> PostActionPlugin<T> createPostActionPlugin(PluginDef def);

    <T> PreActionPlugin<T> createPreActionPlugin(PluginDef def);

    <T> EventHandlerPlugin<T> createEventHandlerPlugin(PluginDef def);
}

