package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.dto.LedgerStateStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.service.LedgerStateStatusService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin-ui")
@RequiredArgsConstructor
@Hidden
public class LedgerStateController {
    private final LedgerStateStatusService ledgerStateStatusService;

    @GetMapping("/ledger-state")
    public ResponseEntity<LedgerStateStatusDto> getLedgerStateStatus() {
        return ResponseEntity.ok(ledgerStateStatusService.getLedgerStateStatus());
    }
}
