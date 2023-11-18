package com.bloxbean.cardano.yaci.store.api.mir.controller;

import com.bloxbean.cardano.yaci.store.api.mir.service.MIRService;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
public class MIRController {
    private final MIRService mirService;

    @GetMapping("/mir")
    @Operation(description = "Get mir summaries by page number and count")
    public List<MoveInstataneousRewardSummary> getMIRSummaries(@RequestParam(name = "page", defaultValue = "0") int page,
                                                                     @RequestParam(name = "count", defaultValue = "10") int count) {
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
