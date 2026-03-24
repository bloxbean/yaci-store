package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.AdminUiProperties;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import com.bloxbean.cardano.yaci.store.core.service.SyncStatusService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin-ui/sync")
@RequiredArgsConstructor
@Hidden
public class SyncStatusController {
    private final SyncStatusService syncStatusService;
    private final StartService startService;
    private final AdminUiProperties adminUiProperties;
    private final StoreProperties storeProperties;

    @GetMapping("/status")
    public ResponseEntity<SyncStatus> getSyncStatus() {
        return ResponseEntity.ok(syncStatusService.getSyncStatus());
    }

    @GetMapping("/control-enabled")
    public ResponseEntity<Map<String, Boolean>> isSyncControlEnabled() {
        boolean enabled = adminUiProperties.isSyncControlEnabled() && !storeProperties.isReadOnlyMode();
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @PostMapping("/start")
    public ResponseEntity<Void> startSync() {
        if (!adminUiProperties.isSyncControlEnabled() || storeProperties.isReadOnlyMode()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!startService.isStarted()) {
            startService.start();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stopSync() {
        if (!adminUiProperties.isSyncControlEnabled() || storeProperties.isReadOnlyMode()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (startService.isStarted()) {
            startService.stop();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restart")
    public ResponseEntity<Void> restartSync() {
        if (!adminUiProperties.isSyncControlEnabled() || storeProperties.isReadOnlyMode()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (startService.isStarted()) {
            startService.stop();
        }
        startService.start();
        return ResponseEntity.ok().build();
    }
}
