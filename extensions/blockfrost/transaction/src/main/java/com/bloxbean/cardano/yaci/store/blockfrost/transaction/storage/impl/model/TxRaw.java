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
public class TxRaw {
    private String txHash;
    private String blockHash;
    private Long blockNumber;
    private Long blockTime;
    private Long slot;
    private Integer txIndex;
    private Integer epoch;
    private BigInteger fees;
    private Long ttl;
    private Long validityIntervalStart;
    private Integer cborSize;
    private Boolean invalid;
    private int withdrawalCount;
    private int delegationCount;
    private int stakeCertCount;
    private int stakeRegCount;
    private int stakeDeregCount;
    private int poolUpdateCount;
    private int poolRetireCount;
    private int redeemerCount;
    private int assetMintOrBurnCount;
    private int inputCount;
    private int outputCount;
    private int collateralReturnCount;
}
