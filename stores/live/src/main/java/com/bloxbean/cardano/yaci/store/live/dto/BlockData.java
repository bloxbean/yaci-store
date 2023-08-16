package com.bloxbean.cardano.yaci.store.live.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BlockData {
    @Builder.Default
    private ResType resType = ResType.BLOCK_DATA;
    private long time;
    private long number;
    private long epoch;
    private int era;
    private long slot;
    private long epochSlot;
    private long slotsPerEpoch;
    private String slotLeader;
    private String hash;
    private long size; //block size in bytes
    private int sizePerct;
    private int nTx;
    private BigInteger fee;
    private long blockTime;
}
