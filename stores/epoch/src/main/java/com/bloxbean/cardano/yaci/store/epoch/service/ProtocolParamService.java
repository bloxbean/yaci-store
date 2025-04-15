package com.bloxbean.cardano.yaci.store.epoch.service;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProtocolParamService {
    private final EpochParamStorage epochParamStorage;

    public Optional<ProtocolParams> getProtocolParam(int epoch) {
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams);
    }

}
