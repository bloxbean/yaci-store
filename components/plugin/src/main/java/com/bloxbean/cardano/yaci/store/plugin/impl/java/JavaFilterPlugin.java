package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;

import java.util.Collection;

/**
 * Base class for java {@link FilterPlugin}s. Extend this and implement {@link #filter(Collection)}.
 *
 * <pre>{@code
 * public class MyFilter extends JavaFilterPlugin<Transaction> {
 *     public MyFilter(PluginDef def, PluginType type, PluginContext ctx) { super(def, type, ctx); }
 *
 *     @Override
 *     public Collection<Transaction> filter(Collection<Transaction> items) {
 *         return items.stream().filter(tx -> tx.getBlock() % 2 == 0).toList();
 *     }
 * }
 * }</pre>
 */
public abstract class JavaFilterPlugin<T> extends AbstractJavaPlugin<T> implements FilterPlugin<T> {

    protected JavaFilterPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    /**
     * Filter the supplied items. Return the items that should continue downstream.
     * Use {@link #state()} / {@link #context()} for persistence, HTTP, DB access, etc.
     */
    @Override
    public abstract Collection<T> filter(Collection<T> items);
}
