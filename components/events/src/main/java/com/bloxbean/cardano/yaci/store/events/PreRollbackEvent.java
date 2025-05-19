package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import lombok.*;

/**
 * Represents an event that occurs prior to the rollback event.
 * This event contains details about the point to which the rollback will occur,
 * the current point in the chain, and the block number at the current point.
 *
 * This can be used to do pre-rollback operations, such as saving the current state.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
public class PreRollbackEvent {
    private Point rollbackTo;
    private Point currentPoint;
    private long currentBlock;

    private boolean remotePublish; //Is published by a remote publisher
}
