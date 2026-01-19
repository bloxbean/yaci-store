package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.dto.SyncStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.service.SyncStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin-ui/sync")
@RequiredArgsConstructor
public class SyncStatusController {
    private final SyncStatusService syncStatusService;

    @GetMapping("/status")
    public ResponseEntity<SyncStatusDto> getSyncStatus() {
        return ResponseEntity.ok(syncStatusService.getSyncStatus());
    }

    @PostMapping("/start")
    public ResponseEntity<Void> startSync() {
        syncStatusService.startSync();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stopSync() {
        syncStatusService.stopSync();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restart")
    public ResponseEntity<Void> restartSync() {
        syncStatusService.restartSync();
        return ResponseEntity.ok().build();
    }
}
