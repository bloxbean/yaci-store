package com.bloxbean.cardano.yaci.store.live.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MempoolTx {
    private String txHash;
    private BigInteger totalFee;
    private BigInteger totalOutput;
}
