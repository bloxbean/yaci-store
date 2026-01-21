package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.dto.IndexStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.service.IndexStatusService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-ui")
@RequiredArgsConstructor
@Hidden
public class IndexController {
    private final IndexStatusService indexStatusService;

    @GetMapping("/indexes")
    public ResponseEntity<List<IndexStatusDto>> getIndexStatuses() {
        return ResponseEntity.ok(indexStatusService.getIndexStatuses());
    }

    @PostMapping("/indexes/refresh")
    public ResponseEntity<List<IndexStatusDto>> refreshIndexStatuses() {
        return ResponseEntity.ok(indexStatusService.getIndexStatuses(true));
    }
}
