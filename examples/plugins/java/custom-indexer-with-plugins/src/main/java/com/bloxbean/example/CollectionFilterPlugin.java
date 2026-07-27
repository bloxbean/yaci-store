package com.bloxbean.example;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.JavaFilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.PluginContext;

import java.util.Collection;

/**
 * Hooks the {@code metadata.save} extension point and keeps only metadata entries whose
 * body mentions {@code "image"} and is longer than 100 characters.
 */
public class CollectionFilterPlugin extends JavaFilterPlugin<TxMetadataLabel> {

    public CollectionFilterPlugin(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    @Override
    public Collection<TxMetadataLabel> filter(Collection<TxMetadataLabel> items) {
        return items.stream()
                .filter(item -> {
                    String body = item.getBody();
                    // SpEL throws on null property access; guard explicitly in Java.
                    return body != null && body.contains("image") && body.length() > 100;
                })
                .toList();
    }
}
