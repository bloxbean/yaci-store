package com.bloxbean.cardano.yaci.store.live.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
   // private long epoch;
    private long slot;
    private String mintedBy;
    private String hash;
    private long size; //block size in bytes
    private int sizePerct;
    private int nTx;
    private long fee;

}
