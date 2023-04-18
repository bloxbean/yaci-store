package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.core.model.Era;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder=true)
@ToString
public class EventMetadata {
    private Era era;
    private long block;
    private String blockHash;
    private long slot;
    private int noOfTxs;
    private boolean syncMode;

    private boolean remotePublish; //Is published by a remote publisher
}
