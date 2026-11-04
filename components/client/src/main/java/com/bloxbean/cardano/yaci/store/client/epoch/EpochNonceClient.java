package com.bloxbean.cardano.yaci.store.client.epoch;

import java.util.Optional;

public interface EpochNonceClient {
    Optional<String> getEpochNonce(int epoch);
}
