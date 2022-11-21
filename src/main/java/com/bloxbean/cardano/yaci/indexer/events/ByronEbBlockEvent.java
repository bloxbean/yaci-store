package com.bloxbean.cardano.yaci.indexer.events;

import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ByronEbBlockEvent {
    private EventMetadata eventMetadata;
    private ByronEbBlock byronEbBlock;
}
