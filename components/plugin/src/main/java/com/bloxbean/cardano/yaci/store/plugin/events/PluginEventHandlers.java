package com.bloxbean.cardano.yaci.store.plugin.events;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.*;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.plugin.core.PluginRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PluginEventHandlers extends PluginBaseEventHandler {

    public PluginEventHandlers(PluginRegistry pluginRegistry, StoreProperties storeProperties) {
        this.pluginRegistry = pluginRegistry;
        this.storeProperties = storeProperties;
    }

    @EventListener
    public void handleRollbackEvent(RollbackEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleCommitEvent(CommitEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handlePreCommitEvent(PreCommitEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handlePreEpochTransitionEvent(PreEpochTransitionEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleEpochTransitionEvent(EpochTransitionCommitEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleAuxDataEvent(AuxDataEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleBlockEvent(BlockEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleCertificateEvent(CertificateEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleEpochChangeEvent(EpochChangeEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleGovernanceEvent(GovernanceEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleMintBurnEvent(MintBurnEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleScriptEvent(ScriptEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleTransactionEvent(TransactionEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleUpdateEvent(UpdateEvent event) {
        handleEvent(event);
    }

}
