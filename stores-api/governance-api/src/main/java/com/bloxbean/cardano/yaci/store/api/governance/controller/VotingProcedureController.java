package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.dto.VotingProcedureDto;
import com.bloxbean.cardano.yaci.store.api.governance.dto.VotingProcedureDtoMapper;
import com.bloxbean.cardano.yaci.store.api.governance.service.VotingProcedureService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "VotingProcedure Service")
@RequestMapping("${apiPrefix}/governance/votes")
@ConditionalOnExpression("${store.governance.endpoints.vote.enabled:true}")
public class VotingProcedureController {
    private final VotingProcedureService votingProcedureService;
    private final VotingProcedureDtoMapper votingProcedureDtoMapper;

    @GetMapping
    @Operation(description = "Get voting procedure list")
    public ResponseEntity<List<VotingProcedureDto>> getVotingProcedureList(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                           @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count,
                                                                           @RequestParam(name = "order", defaultValue = "desc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return ResponseEntity.ok(
                votingProcedureService.getVotingProcedureList(p, count, order).stream()
                        .map(votingProcedureDtoMapper::toVotingProcedureDto)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/txs/{txHash}")
    @Operation(description = "Get voting procedure list by transaction hash")
    public ResponseEntity<List<VotingProcedureDto>> getVotingProcedureByTx(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
        return ResponseEntity.ok(
                votingProcedureService.getVotingProcedureByTx(txHash).stream()
                        .map(votingProcedureDtoMapper::toVotingProcedureDto)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/id/{id}")
    @Operation(description = "Get voting procedure by id")
    public ResponseEntity<VotingProcedureDto> getVotingProcedureById(@PathVariable UUID id) {
        return votingProcedureService.getVotingProcedureById(id)
                .map(votingProcedure -> ResponseEntity.ok(votingProcedureDtoMapper.toVotingProcedureDto(votingProcedure)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found"));
    }
}
