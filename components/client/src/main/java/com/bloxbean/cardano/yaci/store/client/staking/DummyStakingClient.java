package com.bloxbean.cardano.yaci.store.client.staking;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class DummyStakingClient implements StakingClient {

    public DummyStakingClient() {
        log.warn("Dummy Staking Client Configured >>>>>>");
    }

    @Override
    public Optional<String> getStakeAddressFromPointer(long slot, int txIndex, int certIndex) {
        return Optional.empty();
    }
}
