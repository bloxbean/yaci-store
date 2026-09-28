package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.api.IPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.State;

/**
 * Base class for {@code lang=java} plugins. It implements the {@link IPlugin} bookkeeping
 * ({@code getName}, {@code getPluginDef}, {@code getPluginType}) so concrete plugins only have to
 * implement the functional method of their chosen plugin type.
 *
 * <p>Subclasses are expected to declare a public constructor
 * {@code (PluginDef, PluginType, PluginContext)} that forwards to
 * {@link #AbstractJavaPlugin(PluginDef, PluginType, PluginContext)}. The provided
 * {@code Java*Plugin} base classes already do this.</p>
 */
public abstract class AbstractJavaPlugin<T> implements IPlugin<T> {

    protected final PluginDef pluginDef;
    protected final PluginType pluginType;
    protected final PluginContext context;

    protected AbstractJavaPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        this.pluginDef = pluginDef;
        this.pluginType = pluginType;
        this.context = context;
    }

    @Override
    public final String getName() {
        return pluginDef.getName();
    }

    @Override
    public final PluginDef getPluginDef() {
        return pluginDef;
    }

    @Override
    public final PluginType getPluginType() {
        return pluginType;
    }

    /** The context injected at construction. */
    protected PluginContext context() {
        return context;
    }

    /** Convenience for {@code context().state()} (per-plugin state). */
    protected State<String, Object> state() {
        return context.state();
    }

    /** Convenience for {@code context().globalState()}. */
    protected State<String, Object> globalState() {
        return context.globalState();
    }
}
