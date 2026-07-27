package com.bloxbean.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.file.FileOperationResult;
import com.bloxbean.cardano.yaci.store.plugin.file.PluginFileClient;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.JavaFilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.JavaPreActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.PluginContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Hooks the {@code metadata.save} extension point and keeps only metadata
 * labels
 * {@code 721} (NFT metadata) and {@code 1967} (pool metadata).
 */
@Slf4j
public class Example extends JavaFilterPlugin<TxMetadataLabel> {

    public Example(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    @Override
    public Collection<TxMetadataLabel> filter(Collection<TxMetadataLabel> items) {
        // Integer count = context.namedJdbc().queryForObject("select count(*) from
        // transaction_metadata;", Map.of(), Integer.class);
        // System.out.println("transaction metadata number: " + count);
        json();

        return items;
    }

    public void txt(Collection<TxMetadataLabel> items) {

        // Java - File operations with error handling (files() from the PluginContext)
        PluginFileClient files = context.files();

        // Read configuration
        FileOperationResult content = files.read("config.txt");
        if (content.isSuccess()) {
            String configText = content.getAsString();
            log.info("Config loaded: {}", configText);
        }

        // Write processing data
        FileOperationResult result = files.write("output.txt", "Item processed: " + items.size());
        if (result.isSuccess()) {
            log.info("Data written successfully");
        }

        // Append to log file
        files.append("process.log", "Event: " + "test" + "\n");
    }

    public void json() {
        // Java - JSON configuration and logging (files() from the PluginContext)
        PluginFileClient files = context().files();
        var eventId = 1;
        var eventCount = 2;
        var itemId = 1;

        // Read configuration
        FileOperationResult configResult = files.readJson("config.json");
        if (configResult.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) configResult.getData();
            Object apiUrl = config.get("api_url");
            Object timeout = config.get("timeout");
        }

        // Export data
        Map<String, Object> eventData = Map.of(
                "id", eventId,
                "timestamp", System.currentTimeMillis(),
                "count", eventCount);
        files.writeJson("events.json", eventData);

        // Append to event log
        Map<String, Object> event = Map.of(
                "type", "item_processed",
                "item_id", itemId,
                "time", System.currentTimeMillis());
        files.appendJson("events.json", event);

    }

}
