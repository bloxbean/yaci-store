package com.bloxbean.cardano.yaci.store.staking.domain;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.Relay;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PoolRegistration {
    private String txHash;
    private int certIndex;
    private String poolId;
    private String vrfKeyHash;
    private BigInteger pledge;
    private BigInteger cost;
    private double margin;
    private String rewardAccount;
    private Set<String> poolOwners;
    private List<Relay> relays;
    //pool_metadata
    private String metadataUrl;
    private String metadataHash;

    private int epoch;
    private long slot;

    private long block;
    private String blockHash;
    private long blockTime;

    //derived
    public String getRewardAccountBech32() {
        if (rewardAccount == null)
            return "";

        Address address = new Address(HexUtil.decodeHexString(rewardAccount));
        return address.toBech32();
    }

    public String getPoolIdBech32() {
        if (poolId == null)
            return "";
        return Bech32.encode(HexUtil.decodeHexString(poolId), "pool");
    }
}
