package com.bloxbean.cardano.yaci.store.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class GenesisBlockEvent {
    private String blockHash;
    private long blockTime;
    private long block;
    private long slot;

    private boolean remotePublish; //Is published by a remote publisher
}
