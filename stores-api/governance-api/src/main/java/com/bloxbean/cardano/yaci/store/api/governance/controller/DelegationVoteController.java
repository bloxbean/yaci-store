package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.service.DelegationVoteService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Delegation Vote Service")
@RequestMapping("${apiPrefix}/governance/delegation-votes")
@ConditionalOnExpression("${store.governance.endpoints.delegationVote.enabled:true}")
public class DelegationVoteController {
    private final DelegationVoteService delegationVoteService;

    @GetMapping
    @Operation(description = "Get delegation votes by page number and count")
    public ResponseEntity<List<DelegationVote>> getDelegations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                               @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count,
                                                               @RequestParam(name = "order", defaultValue = "desc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return ResponseEntity.ok(delegationVoteService.getDelegations(p, count, order));
    }

    @GetMapping("/drep/{dRepId}")
    @Operation(description = "Get delegations by DRep ID")
    public ResponseEntity<List<DelegationVote>> getDelegationsOfDRep(@PathVariable String dRepId,
                                                                     @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                     @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count,
                                                                     @RequestParam(name = "order", defaultValue = "desc") Order order) {
        int p = page;
        if (p > 0)
            p = p - 1;

        return ResponseEntity.ok(delegationVoteService.getDelegationsByDRepId(dRepId, p, count, order));
    }

    @GetMapping("/address/{address}")
    @Operation(description = "Get delegations by address")
    public ResponseEntity<List<DelegationVote>> getDelegationsByAddress(@PathVariable String address,
                                                                        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                        @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count,
                                                                        @RequestParam(name = "order", defaultValue = "desc") Order order) {
        int p = page;
        if (p > 0)
            p = p - 1;

        return ResponseEntity.ok(delegationVoteService.getDelegationsByAddress(address, p, count, order));
    }
}
