package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.core.model.PoolParams;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * This is only valid for devnets
 */
@Data
@AllArgsConstructor
public class GenesisStaking {
    private List<PoolParams> pools;
    private List<Stake> stakes;

    @Data
    @AllArgsConstructor
    public static class Stake {
        private String stakeKeyHash;
        private String poolHash;
    }
}
