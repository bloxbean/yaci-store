package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.PreActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;

import java.util.Collection;

/**
 * Base class for java {@link PreActionPlugin}s. Extend this and implement
 * {@link #preAction(Collection)}.
 */
public abstract class JavaPreActionPlugin<T> extends AbstractJavaPlugin<T> implements PreActionPlugin<T> {

    protected JavaPreActionPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    /** Run before the target method invocation (and after filter plugins); return the (possibly modified) items. */
    @Override
    public abstract Collection<T> preAction(Collection<T> items);
}
