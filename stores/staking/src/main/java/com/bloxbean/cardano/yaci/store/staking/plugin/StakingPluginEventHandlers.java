package com.bloxbean.cardano.yaci.store.staking.plugin;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.core.PluginRegistry;
import com.bloxbean.cardano.yaci.store.plugin.events.PluginBaseEventHandler;
import com.bloxbean.cardano.yaci.store.staking.domain.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StakingPluginEventHandlers extends PluginBaseEventHandler {
    public StakingPluginEventHandlers(PluginRegistry pluginRegistry, StoreProperties storeProperties) {
        this.pluginRegistry = pluginRegistry;
        this.storeProperties = storeProperties;
    }

    @EventListener
    public void handlePoolRegistrationEvent(PoolRegistrationEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handlePoolRetiredEvent(PoolRetiredEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handlePoolRetirementEvent(PoolRetirementEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleStakeRegDeregEvent(StakeRegDeregEvent event) {
        handleEvent(event);
    }

    @EventListener
    public void handleStakeDepositEvent(StakingDepositEvent event) {
        handleEvent(event);
    }
}
