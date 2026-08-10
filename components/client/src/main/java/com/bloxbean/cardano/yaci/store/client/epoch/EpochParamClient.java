package com.bloxbean.cardano.yaci.store.client.epoch;

import com.bloxbean.cardano.client.api.model.ProtocolParams;

import java.util.Optional;

/**
 * Inter-module client interface for reading the latest protocol parameters in
 * Cardano Client Lib format.
 */
public interface EpochParamClient {

    /**
     * Returns the latest available protocol parameters.
     *
     * @return latest protocol parameters, or {@link Optional#empty()} when they are not available yet
     */
    Optional<ProtocolParams> getLatestProtocolParams();
}
