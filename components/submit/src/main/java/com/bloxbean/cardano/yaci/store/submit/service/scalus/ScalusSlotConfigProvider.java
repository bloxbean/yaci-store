package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import scalus.cardano.ledger.SlotConfig;

@Component
@RequiredArgsConstructor
class ScalusSlotConfigProvider {
    private final ObjectProvider<EraService> eraService;
    private final ObjectProvider<GenesisConfig> genesisConfig;
    private volatile SlotConfig slotConfig;

    SlotConfig getSlotConfig() throws ApiException {
        SlotConfig current = slotConfig;
        if (current != null)
            return current;

        synchronized (this) {
            if (slotConfig == null)
                slotConfig = createSlotConfig();

            return slotConfig;
        }
    }

    private SlotConfig createSlotConfig() throws ApiException {
        EraService eraService = this.eraService.getIfAvailable();
        GenesisConfig genesisConfig = this.genesisConfig.getIfAvailable();
        if (eraService == null || genesisConfig == null) {
            throw new ApiException("Scalus tx evaluator requires EraService and GenesisConfig. Enable the core store configuration.");
        }

        long zeroTime = eraService.shelleyEraStartTime() * 1000;
        long zeroSlot = eraService.getFirstNonByronSlot();
        long slotLength = Math.round(genesisConfig.slotDuration(Era.Shelley) * 1000);

        return new SlotConfig(zeroTime, zeroSlot, slotLength);
    }
}
