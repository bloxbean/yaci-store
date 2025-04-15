package com.bloxbean.cardano.yaci.store.api.adapot.controller;

import com.bloxbean.cardano.yaci.store.api.adapot.dto.AdaPotDto;
import com.bloxbean.cardano.yaci.store.api.adapot.service.AdaPotApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.ControllerPageUtil.adjustPage;

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

    @GetMapping("/list")
    @Operation(description = "Get list of adapots with pagination")
    public List<AdaPotDto> getAdaPots(
            @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count) {

        int p = adjustPage(page);

        return adaPotApiService.getAdaPots(p, count);
    }

    @GetMapping("/epochs/{epoch}")
    @Operation(description = "Get adapot by epoch")
    public AdaPotDto getAdaPotByEpoch(@PathVariable Integer epoch) {
        return adaPotApiService.getAdaPot(epoch)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adapot not found"));
    }
}
