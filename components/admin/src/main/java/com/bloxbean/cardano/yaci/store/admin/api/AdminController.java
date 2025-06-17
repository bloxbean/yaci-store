package com.bloxbean.cardano.yaci.store.admin.api;

import com.bloxbean.cardano.yaci.store.account.service.AddressBalanceSnapshotService;
import com.bloxbean.cardano.yaci.store.account.service.BalanceSnapshotService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("AdminController")
@RequestMapping("${apiPrefix}")
@Slf4j
@ReadOnly(false)
@ConditionalOnExpression("${store.admin.api-enabled:false}")
@Tag(name = "Admin API", description = "Admin API for managing the store")
public class AdminController {
    private final StartService startService;
    private final BalanceSnapshotService balanceSnapshotService;
    private final AddressBalanceSnapshotService addressBalanceSnapshotService;

    public AdminController(@Autowired(required = true) StartService startService,
                           @Autowired(required = false) BalanceSnapshotService balanceSnapshotService,
                           @Autowired(required = false) AddressBalanceSnapshotService addressBalanceSnapshotService) {
        this.startService = startService;
        this.balanceSnapshotService = balanceSnapshotService;
        this.addressBalanceSnapshotService = addressBalanceSnapshotService;
    }

    @PostMapping(value = "/admin/calculate-address-balance", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Calculate address balance for list of addresses and update database")
    public String calculateAddressBalance(@RequestBody List<String> addresses) {
        if (addressBalanceSnapshotService == null)
            return "Address balance snapshot service is not enabled. Skipped";
        else
            return addressBalanceSnapshotService.scheduleBalanceCalculationForAddresses(addresses, false)? "Address balance calculation scheduled" : "Skipped";
    }

    @PostMapping(value = "/admin/calculate-stakeaddress-balance", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Calculate balance for list of stake addresses and update database")
    public String calculateStakeAddressBalance(@RequestBody List<String> stakeAddresses) {
        if (addressBalanceSnapshotService == null)
            return "Address balance snapshot service is not enabled. Skipped";
        else
            return addressBalanceSnapshotService.scheduleBalanceCalculationForAddresses(stakeAddresses, true)? "Stake Address balance calculation scheduled" : "Skipped";
    }

    @PostMapping("/admin/balance-snapshot")
    @Operation(description = "Take Balance snapshot")
    public String takeBalanceSnapshot() {
        if (balanceSnapshotService == null)
            return "Balance snapshot service is not enabled. Skipped";
        else
            return balanceSnapshotService.scheduleBalanceSnapshot()? "Balance snapshot scheduled" : "Skipped";

    }

    @PostMapping("/admin/balance-snapshot/block/{block}/slot/{slot}/hash/{blockHash}")
    @Operation(description = "Take Balance snapshot")
    public String takeBalanceSnapshotAtBlock(@PathVariable long block, @PathVariable long slot, @PathVariable String blockHash) {
        if (balanceSnapshotService  == null) {
            return "Balance snapshot service is not enabled. Skipped";
        } else
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
