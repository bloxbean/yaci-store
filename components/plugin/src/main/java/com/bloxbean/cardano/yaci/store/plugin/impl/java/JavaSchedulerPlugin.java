package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.SchedulerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;

/**
 * Base class for java {@link SchedulerPlugin}s. Extend this and implement {@link #execute()}.
 */
public abstract class JavaSchedulerPlugin<T> extends AbstractJavaPlugin<T> implements SchedulerPlugin<T> {

    protected JavaSchedulerPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }
}
