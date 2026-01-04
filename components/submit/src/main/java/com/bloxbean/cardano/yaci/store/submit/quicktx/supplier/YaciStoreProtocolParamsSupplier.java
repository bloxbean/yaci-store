package com.bloxbean.cardano.yaci.store.submit.quicktx.supplier;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Supplies protocol parameters by reading them from the Yaci Store database.
 */
@Slf4j
@RequiredArgsConstructor
public class YaciStoreProtocolParamsSupplier implements ProtocolParamsSupplier {

    private final EpochParamStorage epochParamStorage;
    private final ObjectMapper objectMapper;

    @Override
    public ProtocolParams getProtocolParams() {
        return epochParamStorage.getLatestEpochParam()
                .map(epochParam -> toClientProtocolParams(epochParam.getParams()))
                .orElseThrow(() -> new IllegalStateException("Protocol parameters are not available. Ensure store.epoch is enabled and synced."));
    }

    private ProtocolParams toClientProtocolParams(com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams params) {
        ProtocolParamsDto dto = DomainMapper.INSTANCE.toProtocolParamsDto(params);
        return objectMapper.convertValue(dto, ProtocolParams.class);
    }
}
