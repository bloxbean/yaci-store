package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${store.extensions.asset-store.api-prefix:/api/v1}")
@ConditionalOnExpression("${store.extensions.asset-store.enabled:false}")
@RequiredArgsConstructor
@Tag(name = "CIP-68 Metadata", description = "On-chain token metadata (fungible and non-fungible) parsed from CIP-68 reference NFT inline datums")
@Slf4j
public class Cip68MetadataController {

    private final Cip68StorageReader cip68StorageReader;

    @Operation(operationId = "getCip68Metadata",
            summary = "Get CIP-68 on-chain fungible token metadata",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reference NFT metadata found"),
                    @ApiResponse(responseCode = "404", description = "No CIP-68 reference NFT found for this policy/asset")
            })
    @GetMapping(path = "/cip68/{policyId}/{assetName}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<FungibleTokenMetadata> getCip68Metadata(
            @Parameter(description = "The policy ID (56 hex characters)")
            @PathVariable("policyId") String policyId,
            @Parameter(description = "The reference NFT asset name (hex, with 000643b0 prefix)")
            @PathVariable("assetName") String assetName) {

        return cip68StorageReader.findByPolicyIdAndAssetName(policyId, assetName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
