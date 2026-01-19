package com.bloxbean.cardano.yaci.store.adminui.controller;

import com.bloxbean.cardano.yaci.store.adminui.dto.StoreStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.service.StoreStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-ui")
@RequiredArgsConstructor
public class StoreStatusController {
    private final StoreStatusService storeStatusService;

    @GetMapping("/stores")
    public ResponseEntity<List<StoreStatusDto>> getStoreStatuses() {
        return ResponseEntity.ok(storeStatusService.getStoreStatuses());
    }
}
