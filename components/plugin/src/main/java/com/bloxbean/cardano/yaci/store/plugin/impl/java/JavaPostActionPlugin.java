package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.PostActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;

import java.util.Collection;

/**
 * Base class for java {@link PostActionPlugin}s. Extend this and implement
 * {@link #postAction(Collection)}.
 */
public abstract class JavaPostActionPlugin<T> extends AbstractJavaPlugin<T> implements PostActionPlugin<T> {

    protected JavaPostActionPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    /** Run after the target method invocation completes. */
    @Override
    public abstract void postAction(Collection<T> items);
}
