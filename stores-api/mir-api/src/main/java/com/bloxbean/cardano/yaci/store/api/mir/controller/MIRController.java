package com.bloxbean.cardano.yaci.store.api.mir.controller;

import com.bloxbean.cardano.yaci.store.api.mir.service.MIRService;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Network Service")
@RequestMapping("${apiPrefix}")
public class MIRController {

    private final MIRService mirService;

    @GetMapping("/mir")
    @Operation(description = "Get mir summaries by page number and count")
    public List<MoveInstataneousRewardSummary> getMIRSummaries(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                               @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return mirService.getMoveInstataneousRewardSummary(p, count);
    }

    @GetMapping("/txs/{txHash}/mir")
    @Operation(description = "Get mirs by tx hash")
    public List<MoveInstataneousReward> getMIRByTxHash(@PathVariable String txHash) {
        return mirService.getMoveInstataneousRewardByTxHash(txHash);
    }
}
