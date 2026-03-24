package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.SyncStatusDto;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Admin UI service for sync status and sync control operations.
 * Delegates sync status retrieval to {@link com.bloxbean.cardano.yaci.store.core.service.SyncStatusService}.
 *
 * @deprecated Use {@link com.bloxbean.cardano.yaci.store.core.service.SyncStatusService} directly for sync status.
 *             This class will be removed in a future release. Sync control operations (start/stop/restart)
 *             will be moved or restructured separately.
 */
@Deprecated
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncStatusService {
    private final com.bloxbean.cardano.yaci.store.core.service.SyncStatusService syncStatusService;
    private final StartService startService;

    public SyncStatusDto getSyncStatus() {
        SyncStatus status = syncStatusService.getSyncStatus();
        return SyncStatusDto.builder()
                .block(status.block())
                .slot(status.slot())
                .epoch(status.epoch())
                .era(status.era())
                .blockHash(status.blockHash())
                .syncPercentage(status.syncPercentage())
                .networkBlock(status.networkBlock())
                .networkSlot(status.networkSlot())
                .synced(status.synced())
                .protocolMagic(status.protocolMagic())
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
