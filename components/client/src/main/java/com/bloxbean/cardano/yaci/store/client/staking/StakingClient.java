package com.bloxbean.cardano.yaci.store.client.staking;

import java.util.Optional;

public interface StakingClient {
    Optional<String> getStakeAddressFromPointer(long slot, int txIndex, int certIndex);
}
