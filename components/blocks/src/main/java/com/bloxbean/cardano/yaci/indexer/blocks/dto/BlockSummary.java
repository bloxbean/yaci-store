package com.bloxbean.cardano.yaci.indexer.blocks.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BlockSummary {
    private long block;
    private long slot;
   // private long epoch;
    private String era;
    private String issuerVkey;
    private long blockBodySize;
    private int noOfTxs;
}
