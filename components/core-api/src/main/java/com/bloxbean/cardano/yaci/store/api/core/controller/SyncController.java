package com.bloxbean.cardano.yaci.store.api.core.controller;

import com.bloxbean.cardano.yaci.store.api.core.dto.SyncStatusDto;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.SyncStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("CoreSyncController")
@RequestMapping("${apiPrefix:/api/v1}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Network API", description = "APIs for Network information")
@ReadOnly(false)
@ConditionalOnExpression("${store.core.endpoints.sync.enabled:true}")
public class SyncController {
    private final SyncStatusService syncStatusService;

    @GetMapping("/sync/status")
    @Operation(description = "Get indexer sync status: store tip vs node tip. network_tip_available indicates whether lag_blocks, sync_percentage, and synced are based on a current node tip.")
    public SyncStatusDto getSyncStatus() {
        return SyncStatusDto.from(syncStatusService.getSyncStatus());
    }
}
