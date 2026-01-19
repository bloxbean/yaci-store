package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.dto.ConfigSectionDto;
import com.bloxbean.cardano.yaci.store.adminui.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-ui")
@RequiredArgsConstructor
public class ConfigController {
    private final ConfigurationService configurationService;

    @GetMapping("/config")
    public ResponseEntity<List<ConfigSectionDto>> getConfiguration() {
        return ResponseEntity.ok(configurationService.getConfiguration());
    }
}
