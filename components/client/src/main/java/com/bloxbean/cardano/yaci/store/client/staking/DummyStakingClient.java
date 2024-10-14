package com.bloxbean.cardano.yaci.store.client.staking;

import java.util.Optional;

public class DummyStakingClient implements StakingClient {
    @Override
    public Optional<String> getStakeAddressFromPointer(long slot, int txIndex, int certIndex) {
        return Optional.empty();
    }
}
