package com.bloxbean.cardano.yaci.store.events.internal;

import com.bloxbean.cardano.yaci.store.events.model.internal.BatchBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchBlocksProcessedEvent {
    private List<BatchBlock> blockCaches;
}
