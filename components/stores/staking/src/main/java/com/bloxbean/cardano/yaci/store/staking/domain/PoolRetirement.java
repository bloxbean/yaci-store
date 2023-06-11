package com.bloxbean.cardano.yaci.store.staking.domain;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PoolRetirement {
    private String txHash;
    private int certIndex;
    private String poolId;
    private int retirementEpoch;

    private int epoch;
    private long slot;

    private long block;
    private String blockHash;
    private long blockTime;

    public String getPoolIdBech32() {
        if (poolId == null)
            return "";
        return Bech32.encode(HexUtil.decodeHexString(poolId), "pool");
    }
}
