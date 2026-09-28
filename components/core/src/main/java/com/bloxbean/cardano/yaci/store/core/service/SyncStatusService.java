package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ReadOnly(false)
@RequiredArgsConstructor
@Slf4j
public class SyncStatusService {
    private final CursorService cursorService;
    private final ChainTipService chainTipService;
    private final EraService eraService;
    private final StoreProperties storeProperties;

    private volatile Tuple<Tip, Integer> cachedTipAndEpoch;
    private volatile long lastTipFetchTime = 0;
    private volatile long lastTipFetchAttemptTime = 0;

    private static final long INITIAL_SYNC_REFRESH_INTERVAL = 15L * 60 * 1000; // 15 minutes
    private static final long SYNCED_REFRESH_INTERVAL = 3L * 60 * 1000;       // 3 minutes
    private static final long FAILED_FETCH_RETRY_INTERVAL = 30L * 1000;       // 30 seconds
    private static final long SYNC_THRESHOLD_BLOCKS = 1000;                   // Consider syncing if > 1000 blocks behind
    private static final long SYNCED_BLOCK_TOLERANCE = 10;                    // Consider synced if within 10 blocks

    public SyncStatus getSyncStatus() {
        Optional<Cursor> cursorOpt = cursorService.getCursor();

        long currentBlock = 0;
        long currentSlot = 0;
        int currentEpoch = 0;
        String era = "Unknown";
        String blockHash = "";

        if (cursorOpt.isPresent()) {
            Cursor cursor = cursorOpt.get();
            currentBlock = cursor.getBlock();
            currentSlot = cursor.getSlot();
            blockHash = cursor.getBlockHash();
            era = cursor.getEra() != null ? cursor.getEra().name() : "Unknown";

            // Calculate current epoch from sync slot
            if (cursor.getEra() != null && cursor.getEra() != Era.Byron) {
                try {
                    currentEpoch = eraService.getEpochNo(cursor.getEra(), currentSlot);
                } catch (Exception e) {
                    // Fall back to 0 if epoch calculation fails
                    log.debug("Could not calculate epoch from slot: {}", e.getMessage());
                }
            }
        }

        long networkBlock = SyncStatus.UNKNOWN_NETWORK_TIP;
        long networkSlot = SyncStatus.UNKNOWN_NETWORK_TIP;

        Optional<Tuple<Tip, Integer>> tipAndEpoch = getCachedTipAndEpoch(currentBlock);
        boolean networkTipAvailable = tipAndEpoch.isPresent();
        if (tipAndEpoch.isPresent()) {
            Tip tip = tipAndEpoch.get()._1;
            networkBlock = tip.getBlock();
            networkSlot = tip.getPoint().getSlot();
        }

        double syncPercentage = 0.0;
        if (networkBlock > 0) {
            long finalNetworkBlock = Math.max(currentBlock, networkBlock);
            syncPercentage = (double) currentBlock / finalNetworkBlock * 100.0;
        }

        boolean isSynced = networkTipAvailable
                && networkBlock > 0
                && currentBlock >= networkBlock - SYNCED_BLOCK_TOLERANCE;

        return SyncStatus.builder()
                .block(currentBlock)
                .slot(currentSlot)
                .epoch(currentEpoch)
                .era(era)
                .blockHash(blockHash)
                .syncPercentage(syncPercentage)
                .networkBlock(networkBlock)
                .networkSlot(networkSlot)
                .synced(isSynced)
                .protocolMagic(storeProperties.getProtocolMagic())
                .build();
    }

    private synchronized Optional<Tuple<Tip, Integer>> getCachedTipAndEpoch(long currentBlock) {
        long now = System.currentTimeMillis();

        // Determine refresh interval based on sync state
        long refreshInterval = INITIAL_SYNC_REFRESH_INTERVAL; // default: syncing
        if (cachedTipAndEpoch != null) {
            long blocksBehind = cachedTipAndEpoch._1.getBlock() - currentBlock;
            if (blocksBehind <= SYNC_THRESHOLD_BLOCKS) {
                refreshInterval = SYNCED_REFRESH_INTERVAL; // at tip: 3 min
            }
        }

        // Return cached if not stale
        if (cachedTipAndEpoch != null && (now - lastTipFetchTime) < refreshInterval) {
            return Optional.of(cachedTipAndEpoch);
        }

        // A failed node lookup can take up to the node-client timeout. Avoid making every
        // status request immediately repeat that lookup while still reporting the tip as unknown.
        if ((now - lastTipFetchAttemptTime) < FAILED_FETCH_RETRY_INTERVAL) {
            return Optional.empty();
        }

        // Fetch fresh tip from node
        lastTipFetchAttemptTime = now;
        try {
            Optional<Tuple<Tip, Integer>> tipAndEpoch = chainTipService.getTipAndCurrentEpoch();
            if (tipAndEpoch.isPresent()) {
                cachedTipAndEpoch = tipAndEpoch.get();
                lastTipFetchTime = now;
            }
            return tipAndEpoch;
        } catch (Exception e) {
            log.debug("Could not get network tip: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
