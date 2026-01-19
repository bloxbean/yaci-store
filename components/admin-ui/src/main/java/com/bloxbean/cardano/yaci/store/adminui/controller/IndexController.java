package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.dto.IndexStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.service.IndexStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-ui")
@RequiredArgsConstructor
public class IndexController {
    private final IndexStatusService indexStatusService;

    @GetMapping("/indexes")
    public ResponseEntity<List<IndexStatusDto>> getIndexStatuses() {
        return ResponseEntity.ok(indexStatusService.getIndexStatuses());
    }
}
