package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
public class RollbackEvent {
    private Point rollbackTo;
    private Point currentPoint;
    private long currentBlock;

    private boolean remotePublish; //Is published by a remote publisher
}
