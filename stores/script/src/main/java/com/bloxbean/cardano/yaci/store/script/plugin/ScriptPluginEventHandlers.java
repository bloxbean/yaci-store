package com.bloxbean.cardano.yaci.store.script.plugin;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginRegistry;
import com.bloxbean.cardano.yaci.store.plugin.events.PluginBaseEventHandler;
import com.bloxbean.cardano.yaci.store.script.domain.DatumEvent;
import com.bloxbean.cardano.yaci.store.script.domain.TxScriptEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ScriptPluginEventHandlers extends PluginBaseEventHandler {
    public ScriptPluginEventHandlers(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @EventListener
    public void handleDatumEvent(DatumEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleTxScriptEvent(TxScriptEvent event) {
        handleEvent(event);
    }
}
