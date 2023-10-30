package com.bloxbean.cardano.yaci.store.events.model.internal;

import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatchByronBlock implements BatchEvent {
    private EventMetadata metadata;
    private ByronMainBlock block;
}
