package com.bloxbean.cardano.yaci.store.events.internal;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * This is an internal event. It is published just before the store starts following the chain block by block
 * (BlockSync): when the bulk fetch from the cursor reaches the tip, or at startup when the cursor is already near
 * the tip. Every block up to {@code from} is committed and no block is being processed. Listeners run synchronously,
 * while BlockFetchService holds its lock, and the sync starts after they return: they must return quickly, so one-off
 * work at the tip (e.g. building address_utxo_unspent) is started in the background from here.
 */
@Getter
@AllArgsConstructor
@Builder
public class PreBlockSyncEvent {
    private Point from;
}
