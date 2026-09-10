package com.bloxbean.example;

import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.file.PluginFileClient;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.JavaEventHandlerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.PluginContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlockEventExamples extends JavaEventHandlerPlugin<BlockEvent> {

    public BlockEventExamples(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    public void filesDir(BlockEvent event) {
        PluginFileClient files = context().files();

        // Create a date-partitioned export directory
        String dateDir = "exports/" + java.time.LocalDate.now().toString();
        files.createDir(dateDir);
        files.writeJson(dateDir + "/block_" + event.getBlock().getHeader().getHeaderBody().getBlockNumber() + ".json",
                event.getBlock().getTransactionBodies());

    }

    public void saveDirPath(BlockEvent event) {
        // Java - Build paths safely (files() from the PluginContext)
        PluginFileClient files = context().files();

        String basePath = "exports";
        String yearMonth = java.time.YearMonth.now().toString().replace("-", "/"); // e.g. 2026/07
        String fileName = "block_" + event.getBlock().getHeader().getHeaderBody().getBlockNumber() + ".json";

        // Cross-platform path construction
        String fullPath = files.joinPath(basePath, yearMonth, fileName);
        files.createDir(files.joinPath(basePath, yearMonth));
        files.writeJson(fullPath, event.getBlock().getTransactionBodies());

        // Extract filename for logging
        String savedFile = files.getFileName(fullPath);
        log.info("Data saved to: {}", savedFile);

    }

    @Override
    public void handleEvent(Object event) {
        if (event instanceof BlockEvent blockEvent) {
            // filesDir(blockEvent);
            saveDirPath(blockEvent);
        } else {
            log.warn("Not a transaction event");
        }
    }

}
