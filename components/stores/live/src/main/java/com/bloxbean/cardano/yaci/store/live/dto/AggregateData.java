package com.bloxbean.cardano.yaci.store.live.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AggregateData {
    @Builder.Default
    private ResType resType = ResType.AGGR_DATA;
    private int durationMin;
    private long time;
    private long totalFee;
    private long totalBlockSize;
    private long avgBlockSize;
    private int blockSizePerct;
    private double tps;
    private double tops;
    private long nMintTokens;
    private long nTxs;
}
