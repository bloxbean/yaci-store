package com.bloxbean.cardano.yaci.store.client.epoch;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class DummyEpochNonceClient implements EpochNonceClient {

    public DummyEpochNonceClient() {
        log.warn("Dummy Epoch Nonce Client Configured >>>>>>");
    }

    @Override
    public Optional<String> getEpochNonce(int epoch) {
        return Optional.empty();
    }
}
