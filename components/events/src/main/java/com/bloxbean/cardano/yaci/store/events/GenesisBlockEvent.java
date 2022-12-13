package com.bloxbean.cardano.yaci.store.events;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenesisBlockEvent {
    private String blockHash;
    private long block;
    private long slot;
}
