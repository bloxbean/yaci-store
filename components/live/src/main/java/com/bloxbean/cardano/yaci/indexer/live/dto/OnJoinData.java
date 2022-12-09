package com.bloxbean.cardano.yaci.indexer.live.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OnJoinData {
    @Builder.Default
    private ResType resType = ResType.INIT_DATA;
    private List<BlockData> blocks;
    private AggregateData aggregateData;
}
