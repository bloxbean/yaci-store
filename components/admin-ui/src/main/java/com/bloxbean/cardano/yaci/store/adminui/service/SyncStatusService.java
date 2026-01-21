package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.store.adminui.dto.SyncStatusDto;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.service.ChainTipService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncStatusService {
    private final CursorService cursorService;
    private final ChainTipService chainTipService;
    private final StartService startService;
    private final EraService eraService;

    private volatile Tuple<Tip, Integer> cachedTipAndEpoch;
    private volatile long lastTipFetchTime = 0;

    private static final long INITIAL_SYNC_REFRESH_INTERVAL = 15 * 60 * 1000; // 15 minutes
    private static final long SYNCED_REFRESH_INTERVAL = 3 * 60 * 1000;        // 3 minutes
    private static final long SYNC_THRESHOLD_BLOCKS = 1000;                   // Consider syncing if > 1000 blocks behind

    public SyncStatusDto getSyncStatus() {
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

        long networkBlock = currentBlock;
        long networkSlot = currentSlot;

        Optional<Tuple<Tip, Integer>> tipAndEpoch = getCachedTipAndEpoch(currentBlock);
        if (tipAndEpoch.isPresent()) {
            Tip tip = tipAndEpoch.get()._1;
            networkBlock = tip.getBlock();
            networkSlot = tip.getPoint().getSlot();
            // No longer overwrite currentEpoch with networkEpoch
        }

        double syncPercentage = 0.0;
        if (networkBlock > 0) {
            long finalNetworkBlock = currentBlock > networkBlock ? currentBlock : networkBlock;
            syncPercentage = (double) currentBlock / finalNetworkBlock * 100.0;
        }

        boolean isSynced = networkBlock > 0 && currentBlock >= networkBlock - 10; // Allow 10 block tolerance

        return SyncStatusDto.builder()
                .block(currentBlock)
                .slot(currentSlot)
                .epoch(currentEpoch)
                .era(era)
                .blockHash(blockHash)
                .syncPercentage(syncPercentage)
                .networkBlock(networkBlock)
                .networkSlot(networkSlot)
                .synced(isSynced)
                .build();
    }

    public void startSync() {
        if (!startService.isStarted()) {
            startService.start();
        }
    }

    public void stopSync() {
        if (startService.isStarted()) {
            startService.stop();
        }
    }

    public void restartSync() {
        if (startService.isStarted()) {
            startService.stop();
        }
        startService.start();
    }

    private Optional<Tuple<Tip, Integer>> getCachedTipAndEpoch(long currentBlock) {
        long now = System.currentTimeMillis();

        // If cursor block is newer than cached tip, no need to fetch from node
        if (cachedTipAndEpoch != null && currentBlock >= cachedTipAndEpoch._1.getBlock()) {
            return Optional.of(cachedTipAndEpoch);
        }

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

        // Fetch fresh tip from node
        try {
            Optional<Tuple<Tip, Integer>> tipAndEpoch = chainTipService.getTipAndCurrentEpoch();
            if (tipAndEpoch.isPresent()) {
                cachedTipAndEpoch = tipAndEpoch.get();
                lastTipFetchTime = now;
            }
            return tipAndEpoch;
        } catch (Exception e) {
            log.debug("Could not get network tip: {}", e.getMessage());
            return Optional.ofNullable(cachedTipAndEpoch); // Return stale cache if available
        }
    }
}
