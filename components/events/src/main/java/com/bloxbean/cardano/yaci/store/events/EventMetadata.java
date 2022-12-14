package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.core.model.Era;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
public class EventMetadata {
    private Era era;
    private long block;
    private String blockHash;
    private long slot;
    private int noOfTxs;
    private boolean isSyncMode;
}
