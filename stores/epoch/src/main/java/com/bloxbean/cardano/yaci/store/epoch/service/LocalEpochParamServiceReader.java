package com.bloxbean.cardano.yaci.store.epoch.service;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.core.annotation.LocalSupport;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.LocalEpochParamsStorage;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@LocalSupport
public class LocalEpochParamServiceReader {
    private final LocalEpochParamsStorage localProtocolParamsStorage;

    public LocalEpochParamServiceReader(LocalEpochParamsStorage localProtocolParamsStorage) {
        this.localProtocolParamsStorage = localProtocolParamsStorage;
    }

    public Optional<ProtocolParams> getCurrentProtocolParams() {
        return localProtocolParamsStorage.getLatestEpochParam()
                .map(EpochParam::getParams);
    }

    public Optional<ProtocolParams> getProtocolParams(int epoch) {
        return localProtocolParamsStorage.getEpochParam(epoch)
                .map(EpochParam::getParams);
    }

    public Optional<Integer> getMaxEpoch() {
        return localProtocolParamsStorage.getMaxEpoch();
    }

}
