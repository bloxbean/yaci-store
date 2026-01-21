package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.dto.HealthStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.service.HealthStatusService;
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
public class HealthController {
    private final HealthStatusService healthStatusService;

    @GetMapping("/health")
    public ResponseEntity<HealthStatusDto> getHealthStatus() {
        return ResponseEntity.ok(healthStatusService.getHealthStatus());
    }
}
