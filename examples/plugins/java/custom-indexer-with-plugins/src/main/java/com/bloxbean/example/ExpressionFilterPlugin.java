package com.bloxbean.example;


import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.JavaFilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.PluginContext;

import java.util.Collection;

/**
 * Hooks the {@code metadata.save} extension point and keeps only metadata labels
 * {@code 721} (NFT metadata) and {@code 1967} (pool metadata).
 */
public class ExpressionFilterPlugin extends JavaFilterPlugin<TxMetadataLabel> {

    public ExpressionFilterPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    @Override
    public Collection<TxMetadataLabel> filter(Collection<TxMetadataLabel> items) {
        return items.stream()
                .filter(item -> "721".equals(item.getLabel()) || "1967".equals(item.getLabel()))
                .toList();
    }
}
