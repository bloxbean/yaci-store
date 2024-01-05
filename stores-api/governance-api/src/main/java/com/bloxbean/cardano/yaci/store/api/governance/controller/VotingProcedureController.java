package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.service.VotingProcedureService;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "VotingProcedure Service")
@RequestMapping("${apiPrefix}/voting-procedure")
@ConditionalOnExpression("${store.governance.endpoints.votingProcedure.enabled:true}")
public class VotingProcedureController {
    private final VotingProcedureService votingProcedureService;

    @GetMapping("/{txHash}")
    @Operation(description = "Get voting procedure list by transaction hash")
    public ResponseEntity<List<VotingProcedure>> getVotingProcedureByTx(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
        return ResponseEntity.ok(votingProcedureService.getVotingProcedureByTx(txHash));
    }

}
