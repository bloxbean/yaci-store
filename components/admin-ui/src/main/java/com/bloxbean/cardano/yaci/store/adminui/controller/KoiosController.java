package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.dto.KoiosTotalsDto;
import com.bloxbean.cardano.yaci.store.adminui.service.KoiosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin-ui/koios")
@RequiredArgsConstructor
public class KoiosController {
    private final KoiosService koiosService;

    @GetMapping("/verification-enabled")
    public ResponseEntity<Map<String, Boolean>> isVerificationEnabled() {
        return ResponseEntity.ok(Map.of("enabled", koiosService.isVerificationAvailable()));
    }

    @GetMapping("/totals")
    public ResponseEntity<KoiosTotalsDto> getTotals(@RequestParam("epoch") int epoch) {
        if (!koiosService.isVerificationAvailable()) {
            return ResponseEntity.notFound().build();
        }

        Optional<KoiosTotalsDto> totals = koiosService.getTotals(epoch);
        return totals.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
