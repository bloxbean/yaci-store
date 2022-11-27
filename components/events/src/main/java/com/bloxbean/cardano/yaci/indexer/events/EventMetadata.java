package com.bloxbean.cardano.yaci.indexer.events;

import com.bloxbean.cardano.yaci.core.model.Era;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EventMetadata {
    private Era era;
    private long block;
    private String blockHash;
    private long slot;
    private boolean isSyncMode;
}
