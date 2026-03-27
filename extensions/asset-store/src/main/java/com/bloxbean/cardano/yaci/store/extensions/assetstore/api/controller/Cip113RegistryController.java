package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service.Cip113RegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${store.extensions.asset-store.api-prefix:/api/v1}")
@ConditionalOnExpression("${store.extensions.asset-store.enabled:false}")
@RequiredArgsConstructor
@Slf4j
public class Cip113RegistryController {

    private final Cip113RegistryService cip113RegistryService;

    @Operation(operationId = "getCip113Registry", summary = "Get CIP-113 programmable token registry entry by policy ID")
    @GetMapping(path = "/cip113/registry/{policyId}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<ProgrammableTokenCip113> getRegistryEntry(
            @Parameter(description = "the policy ID to look up")
            @PathVariable("policyId") String policyId) {

        log.info("CIP-113 registry lookup for policyId: {}", policyId);

        return cip113RegistryService.findByPolicyId(policyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(operationId = "queryCip113Registry", summary = "Batch query CIP-113 programmable token registry entries")
    @PostMapping(value = "/cip113/registry/query", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    public ResponseEntity<Map<String, ProgrammableTokenCip113>> queryRegistryEntries(
            @RequestBody List<String> policyIds) {

        log.info("CIP-113 batch registry lookup for {} policy IDs", policyIds.size());

        Map<String, ProgrammableTokenCip113> results = cip113RegistryService.findByPolicyIds(policyIds);
        return ResponseEntity.ok(results);
    }

}
