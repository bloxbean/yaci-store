package com.bloxbean.cardano.yaci.store.api.adapot.controller;

import com.bloxbean.cardano.yaci.store.api.adapot.dto.AdaPotDto;
import com.bloxbean.cardano.yaci.store.api.adapot.service.AdaPotApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController("AdaPotController")
@RequestMapping("${apiPrefix}/adapot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AdaPot API", description = "APIs for adapot related data.")
public class AdaPotController {
    private final AdaPotApiService adaPotApiService;

    @GetMapping
    @Operation(description = "Get latest adapot")
    public AdaPotDto getAdaPot() {
        return adaPotApiService.getAdaPot()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adapot not found"));
    }

    @GetMapping("/epochs/{epoch}")
    @Operation(description = "Get adapot by epoch")
    public AdaPotDto getAdaPotByEpoch(@PathVariable Integer epoch) {
        return adaPotApiService.getAdaPot(epoch)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adapot not found"));
    }
}
