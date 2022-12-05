package com.bloxbean.cardano.yaci.indexer.events;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RollbackEvent {
    private Point rollbackTo;
    private Point currentPoint;
    private long currentBlock;
}
