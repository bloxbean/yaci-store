package com.bloxbean.cardano.yaci.store.transaction.plugin;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginRegistry;
import com.bloxbean.cardano.yaci.store.plugin.events.PluginBaseEventHandler;
import com.bloxbean.cardano.yaci.store.transaction.domain.event.TxnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionPluginEventHandlers extends PluginBaseEventHandler {
    public TransactionPluginEventHandlers(PluginRegistry pluginRegistry, StoreProperties storeProperties) {
        this.pluginRegistry = pluginRegistry;
        this.storeProperties = storeProperties;
    }

    @EventListener
    public void handleTxnEvent(TxnEvent event) {
        handleEvent(event);
    }
}
