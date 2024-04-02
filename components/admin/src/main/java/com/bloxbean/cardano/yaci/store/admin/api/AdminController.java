package com.bloxbean.cardano.yaci.store.admin.api;

import com.bloxbean.cardano.yaci.store.account.service.BalanceSnapshotService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController("AdminController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${store.admin.api-enabled:false}")
@Tag(name = "Admin API", description = "Admin API for managing the store")
public class AdminController {

    private final BalanceSnapshotService balanceSnapshotService;
    private final StartService startService;

    @PostMapping("/admin/balance-snapshot")
    @Operation(description = "Take Balance snapshot")
    public String takeBalanceSnapshot() {
        return balanceSnapshotService.scheduleBalanceSnapshot()? "Balance snapshot scheduled" : "Skipped";

    }

    @PostMapping("/admin/balance-snapshot/block/{block}/slot/{slot}/hash/{blockHash}")
    @Operation(description = "Take Balance snapshot")
    public String takeBalanceSnapshotAtBlock(@PathVariable long block, @PathVariable long slot, @PathVariable String blockHash) {
        return balanceSnapshotService.scheduleBalanceSnapshot(block, slot, blockHash)? "Balance snapshot scheduled" : "Skipped";
    }

    @PostMapping("/admin/restart-sync")
    @Operation(description = "Restart Sync")
    @SneakyThrows
    public String restartSync() {
        if (startService.isStarted()) {
            startService.stop();
            TimeUnit.SECONDS.sleep(2);
            startService.start();

            return "Sync restarted";
        } else {
            return "Skipped";
        }
    }

    @PostMapping("/admin/stop-sync")
    @Operation(description = "Stop Sync")
    @SneakyThrows
    public String stopSync() {
        if (startService.isStarted()) {
            startService.stop();
            log.info("Sync stopped !!!");

            return "Sync stopped";
        } else {
            return "Skipped as sync is not running";
        }
    }

    @PostMapping("/admin/start-sync")
    @Operation(description = "Start Sync")
    @SneakyThrows
    public String startSync() {
        if (!startService.isStarted()) {
            startService.start();
            log.info("Sync started !!!");

            return "Sync started";
        } else {
            return "Skipped as sync is already running";
        }
    }

}
