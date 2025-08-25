package com.bloxbean.cardano.yaci.store.plugin.api;

import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.ScriptRef;

import java.util.List;

public interface PluginFactory {
    String getLang();

    void initGlobalScripts(List<ScriptRef> scriptRef);

    <T> InitPlugin createInitPlugin(PluginDef def);

    <T> FilterPlugin<T> createFilterPlugin(PluginDef def);

    <T> PostActionPlugin<T> createPostActionPlugin(PluginDef def);

    <T> PreActionPlugin<T> createPreActionPlugin(PluginDef def);

    <T> EventHandlerPlugin<T> createEventHandlerPlugin(PluginDef def);

    <T> SchedulerPlugin<T> createSchedulerPlugin(PluginDef def);
}

