package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxPoolUpdateRaw {
    private Integer certIndex;
    /** Raw hex pool ID from DB (not yet bech32-encoded) */
    private String poolIdHex;
    private String vrfKey;
    private BigInteger pledge;
    private Double margin;
    private BigInteger cost;
    private String rewardAccount;
    /** Raw JSON string of pool owner stake key hashes */
    private String poolOwnersJson;
    /** Raw JSON string of pool relays */
    private String relaysJson;
    private String metadataUrl;
    private String metadataHash;
    private Integer epoch;
}
