package com.bloxbean.cardano.yaci.store.live.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OnJoinData {
    @Builder.Default
    private ResType resType = ResType.INIT_DATA;
    private List<BlockData> blocks;
    private List<RecentTx> recentTxs;
    private AggregateData aggregateData;
}
