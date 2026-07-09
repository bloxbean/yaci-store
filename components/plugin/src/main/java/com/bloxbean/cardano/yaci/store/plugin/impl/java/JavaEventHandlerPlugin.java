package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.api.EventHandlerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;

/**
 * Base class for java {@link EventHandlerPlugin}s. Extend this and implement
 * {@link #handleEvent(Object)}.
 */
public abstract class JavaEventHandlerPlugin<T> extends AbstractJavaPlugin<T> implements EventHandlerPlugin<T> {

    protected JavaEventHandlerPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    /** Handle a published blockchain event. */
    @Override
    public abstract void handleEvent(Object event);
}
