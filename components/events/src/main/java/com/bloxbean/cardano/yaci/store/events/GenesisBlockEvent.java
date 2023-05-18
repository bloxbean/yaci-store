package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.core.model.Era;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class GenesisBlockEvent {
    private String blockHash;
    private long blockTime;
    private long block;
    private long slot;
    private Era era;

    private List<GenesisBalance> genesisBalances;

    private boolean remotePublish; //Is published by a remote publisher
}
