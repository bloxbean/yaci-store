package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.AdminUiProperties;
import com.bloxbean.cardano.yaci.store.adminui.dto.ConfigSectionDto;
import com.bloxbean.cardano.yaci.store.adminui.dto.UiSettingsDto;
import com.bloxbean.cardano.yaci.store.adminui.service.ConfigurationService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-ui")
@RequiredArgsConstructor
@Hidden
public class ConfigController {
    private final ConfigurationService configurationService;
    private final AdminUiProperties adminUiProperties;

    @GetMapping("/config")
    public ResponseEntity<List<ConfigSectionDto>> getConfiguration() {
        return ResponseEntity.ok(configurationService.getConfiguration());
    }

    @GetMapping("/settings")
    public ResponseEntity<UiSettingsDto> getUiSettings() {
        return ResponseEntity.ok(UiSettingsDto.builder()
                .headerText(adminUiProperties.getHeaderText())
                .build());
    }
}
