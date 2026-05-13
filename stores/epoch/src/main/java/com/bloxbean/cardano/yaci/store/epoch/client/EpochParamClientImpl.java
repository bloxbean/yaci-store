package com.bloxbean.cardano.yaci.store.epoch.client;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.store.client.epoch.EpochParamClient;
import com.bloxbean.cardano.yaci.store.common.ccl.CclProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Local {@link EpochParamClient} implementation backed by epoch storage.
 */
@Component("epochParamClient")
@Primary
@RequiredArgsConstructor
@Slf4j
public class EpochParamClientImpl implements EpochParamClient {
    private final EpochParamStorage epochParamStorage;

    @Override
    public Optional<ProtocolParams> getLatestProtocolParams() {
        return epochParamStorage.getLatestEpochParam()
                .map(EpochParam::getParams)
                .map(CclProtocolParamsMapper::toCclProtocolParams);
    }
}
