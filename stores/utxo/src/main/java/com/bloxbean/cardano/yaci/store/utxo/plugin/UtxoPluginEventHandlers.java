package com.bloxbean.cardano.yaci.store.utxo.plugin;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginRegistry;
import com.bloxbean.cardano.yaci.store.plugin.events.PluginBaseEventHandler;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UtxoPluginEventHandlers extends PluginBaseEventHandler {
    public UtxoPluginEventHandlers(PluginRegistry pluginRegistry, StoreProperties storeProperties) {
        this.pluginRegistry = pluginRegistry;
        this.storeProperties = storeProperties;
    }

    @EventListener
    public void handleAddressUtxoEvent(AddressUtxoEvent event) {
        handleEvent(event);
    }
}
