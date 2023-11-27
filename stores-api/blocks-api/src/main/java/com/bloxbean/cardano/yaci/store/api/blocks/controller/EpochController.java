package com.bloxbean.cardano.yaci.store.api.blocks.controller;

import com.bloxbean.cardano.yaci.store.api.blocks.service.EpochReadService;
import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Tag(name = "Epoch Service")
@RestController("EpochController")
@RequestMapping("${apiPrefix}/epochs")
@ConditionalOnExpression("${store.blocks.endpoints.epoch.enabled:true}")
public class EpochController {

    private final EpochReadService epochService;

    public EpochController(EpochReadService epochService) {
        this.epochService = epochService;
    }

    @GetMapping("{number}")
    @Operation(summary = "Specific Epoch Information", description = "Get the information of a specific epoch.")
    public Epoch getEpochByNumber(@PathVariable int number) {
        return epochService.getEpochByNumber(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Epoch not found"));
    }

    @GetMapping
    @Operation(summary = "Epoch List", description = "Get epochs by page number and count.")
    public EpochsPage getEpochs(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return epochService.getEpochs(p, count);
    }
}
