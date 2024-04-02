package com.bloxbean.cardano.yaci.store.api.account.controller;

import com.bloxbean.cardano.yaci.store.account.service.BalanceSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("AdminController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${store.enable-admin-api:false}")
@Tag(name = "Admin API", description = "Admin API for managing the store")
public class AccountAdminController {

    private final BalanceSnapshotService balanceSnapshotService;

    @PostMapping("/admin/balance-snapshot")
    @Operation(description = "Take Balance snapshot")
    public String takeBalanceSnapshot() {
        balanceSnapshotService.scheduleBalanceSnapshot();

        return "Balance snapshot scheduled";
    }

}
