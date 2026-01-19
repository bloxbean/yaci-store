package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.store.adminui.dto.SyncStatusDto;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.service.ChainTipService;
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
        }

        long networkBlock = currentBlock;
        long networkSlot = currentSlot;
        int networkEpoch = currentEpoch;

        try {
            Optional<Tuple<Tip, Integer>> tipAndEpoch = chainTipService.getTipAndCurrentEpoch();
            if (tipAndEpoch.isPresent()) {
                Tip tip = tipAndEpoch.get()._1;
                networkBlock = tip.getBlock();
                networkSlot = tip.getPoint().getSlot();
                networkEpoch = tipAndEpoch.get()._2;
                currentEpoch = networkEpoch; // Use network epoch as current
            }
        } catch (Exception e) {
            log.debug("Could not get network tip: {}", e.getMessage());
        }

        double syncPercentage = 0.0;
        if (networkBlock > 0) {
            syncPercentage = (double) currentBlock / networkBlock * 100.0;
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
}
