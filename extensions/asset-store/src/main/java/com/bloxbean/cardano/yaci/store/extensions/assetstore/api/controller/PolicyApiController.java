package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2.PolicyBatchRequest;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2.PolicyResponse;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Policy API — look up all tokens and extensions for a minting policy.
 * <p>
 * Ported from cf-token-metadata-registry's {@code PolicyApiController}.
 * Hardcoded at {@code /api/v2} for compatibility.
 */
@RestController
@RequestMapping("/api/v2")
@ConditionalOnExpression("${store.extensions.asset-store.enabled:false}")
@RequiredArgsConstructor
@Tag(name = "Policy Service", description = "Policy-level token metadata lookups")
@Slf4j
public class PolicyApiController {

    private final PolicyService policyService;

    @Operation(operationId = "getPolicy",
            summary = "Look up a policy: returns all tokens and programmable token status for the given minting policy")
    @GetMapping(path = "/policies/{policyId}", produces = "application/json;charset=utf-8")
    public ResponseEntity<PolicyResponse> getPolicy(
            @Parameter(description = "the minting policy ID (56 hex characters)")
            @PathVariable("policyId") String policyId) {

        log.info("Policy lookup for policyId: {}", policyId);

        return policyService.findByPolicyId(policyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(operationId = "queryPolicies",
            summary = "Batch lookup of policies: returns tokens and programmable token status for each policy")
    @PostMapping(path = "/policies/query",
            produces = "application/json;charset=utf-8",
            consumes = "application/json;charset=utf-8")
    public ResponseEntity<List<PolicyResponse>> queryPolicies(
            @Valid @RequestBody PolicyBatchRequest body) {

        List<PolicyResponse> results = policyService.findByPolicyIds(body.policyIds());
        return ResponseEntity.ok(results);
    }
}
