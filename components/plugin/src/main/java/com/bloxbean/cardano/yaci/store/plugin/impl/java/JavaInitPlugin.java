package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.api.InitPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;

/**
 * Base class for java {@link InitPlugin}s. Extend this and implement {@link #initPlugin()}.
 * Only one init plugin may be registered per plugin type/lang.
 */
public abstract class JavaInitPlugin<T> extends AbstractJavaPlugin<T> implements InitPlugin<T> {

    protected JavaInitPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }
}
