package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "CIP-113 Registry", description = "Programmable token registry nodes with transfer logic scripts")
@Slf4j
public class Cip113RegistryController {

    private final Cip113StorageReader cip113StorageReader;

    @Operation(operationId = "getCip113Registry",
            summary = "Get CIP-113 programmable token registry entry by policy ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Registry node found"),
                    @ApiResponse(responseCode = "404", description = "Policy is not registered as a programmable token")
            })
    @GetMapping(path = "/cip113/registry/{policyId}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<ProgrammableTokenCip113> getRegistryEntry(
            @Parameter(description = "The minting policy ID (56 hex characters)")
            @PathVariable("policyId") String policyId) {

        return cip113StorageReader.findByPolicyId(policyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(operationId = "queryCip113Registry",
            summary = "Batch query CIP-113 programmable token registry entries",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Map of policyId to registry entry (unregistered policies omitted)")
            })
    @PostMapping(value = "/cip113/registry/query", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    public ResponseEntity<Map<String, ProgrammableTokenCip113>> queryRegistryEntries(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of policy IDs to look up")
            @RequestBody List<String> policyIds) {

        Map<String, ProgrammableTokenCip113> results = cip113StorageReader.findByPolicyIds(policyIds);
        return ResponseEntity.ok(results);
    }
}
