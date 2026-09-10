package com.bloxbean.example;

import java.util.ArrayList;
import java.util.List;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.file.PluginFileClient;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.JavaEventHandlerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.PluginContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionEventExample extends JavaEventHandlerPlugin<TransactionEvent> {

    public TransactionEventExample(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    public void csv(TransactionEvent event) {

        // Java - Export event data to CSV (files() from the PluginContext)
        PluginFileClient files = context().files();

        List<String> headers = List.of("block_hash", "tx_hash", "fee");
        List<List<?>> rows = new ArrayList<>();
        for (Transaction transaction : event.getTransactions()) {
            rows.add(List.of(
                    event.getMetadata().getBlockHash(),
                    transaction.getTxHash(),
                    String.valueOf(transaction.getBody().getFee())));
        }

        // Write new CSV file
        files.writeCsv("transactions.csv", headers, rows, false);

        // Or append to existing CSV
        files.appendCsv("all_transactions.csv", rows);
    }

    @Override
    public void handleEvent(Object event) {
        if (event instanceof TransactionEvent transactionEvent) {
            csv(transactionEvent);
        } else {
            log.warn("Not a transaction event");
        }
    }

}
