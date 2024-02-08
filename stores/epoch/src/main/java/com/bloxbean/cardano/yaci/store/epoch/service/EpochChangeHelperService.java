package com.bloxbean.cardano.yaci.store.epoch.service;

import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper service to check if the epoch change event is a new epoch. This method is used by other stores to confirm if the
 * epoch change event is a new epoch or not.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EpochChangeHelperService {
    private final EpochParamStorage epochParamStorage;

    public boolean isNewEpoch(EpochChangeEvent epochChangeEvent) {
        var dbEpochParam = epochParamStorage.getLatestEpochParam();
        Integer dbEpoch = dbEpochParam.map(EpochParam::getEpoch).orElse(null);
        Long dbSlot = dbEpochParam.map(EpochParam::getSlot).orElse(0L);

        //Verify it with the db value as the prevEpoch is null which could be due to restart
        if (epochChangeEvent.getPreviousEpoch() == null && dbEpoch == epochChangeEvent.getEpoch()
                && dbSlot == epochChangeEvent.getEventMetadata().getSlot()) {
            return true;
        }

        return false;
    }
}

