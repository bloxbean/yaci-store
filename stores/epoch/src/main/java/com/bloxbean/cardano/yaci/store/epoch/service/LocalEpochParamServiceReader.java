package com.bloxbean.cardano.yaci.store.epoch.service;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.LocalEpochParamsStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
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
