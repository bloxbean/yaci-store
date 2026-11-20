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

    /**
     * True when the block at {@link #rollbackTo} is going to be processed again, as on a restart that
     * refetches the cursor block. Stores that rewrite a block's rows idempotently can ignore it. Stores
     * whose rows have generated keys must then delete the rows at {@code rollbackTo} as well, or that
     * block's rows are written twice.
     */
    private boolean rollbackPointReprocessed;

    /**
     * The slot after which a store whose rows have generated keys (and so are not overwritten when a
     * block is processed again) must delete them.
     */
    public long generatedKeyRollbackSlot() {
        return rollbackPointReprocessed ? rollbackTo.getSlot() - 1 : rollbackTo.getSlot();
    }
}
