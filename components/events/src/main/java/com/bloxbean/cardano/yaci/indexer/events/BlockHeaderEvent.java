package com.bloxbean.cardano.yaci.indexer.events;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlockHeaderEvent {
    private EventMetadata metadata;
    private BlockHeader blockHeader;
}
