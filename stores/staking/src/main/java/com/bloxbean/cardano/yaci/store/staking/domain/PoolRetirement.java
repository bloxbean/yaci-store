package com.bloxbean.cardano.yaci.store.staking.domain;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PoolRetirement extends BlockAwareDomain {
    private String txHash;
    private int certIndex;
    private int txIndex;
    private String poolId;
    private int retirementEpoch;

    private int epoch;
    private long slot;
    private String blockHash;

    public String getPoolIdBech32() {
        if (poolId == null)
            return "";
        return Bech32.encode(HexUtil.decodeHexString(poolId), "pool");
    }
}
