package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68FungibleTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${store.extensions.asset-store.api-prefix:/api/v1}")
@ConditionalOnExpression("${store.extensions.asset-store.enabled:false}")
@RequiredArgsConstructor
@Slf4j
public class Cip68MetadataController {

    private final Cip68FungibleTokenService cip68FungibleTokenService;

    @Operation(operationId = "getCip68Metadata", summary = "Get CIP-68 on-chain fungible token metadata")
    @GetMapping(path = "/cip68/{policyId}/{assetName}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<FungibleTokenMetadata> getCip68Metadata(
            @Parameter(description = "the policy ID (56 hex characters)")
            @PathVariable("policyId") String policyId,
            @Parameter(description = "the asset name (hex)")
            @PathVariable("assetName") String assetName) {

        log.info("CIP-68 metadata lookup for policyId: {}, assetName: {}", policyId, assetName);

        return cip68FungibleTokenService.findSubject(policyId, assetName, List.of())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
