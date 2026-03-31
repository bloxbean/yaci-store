package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix:/api/v1}")
@RequiredArgsConstructor
@Tag(name = "CIP-68 Metadata", description = "On-chain token metadata parsed from CIP-68 reference NFT inline datums")
@Slf4j
public class Cip68MetadataController {

    private final Cip68StorageReader cip68StorageReader;

    @Operation(operationId = "getCip68FungibleTokenMetadata",
            summary = "Get CIP-68 fungible token metadata by policy ID and asset name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fungible token metadata found"),
                    @ApiResponse(responseCode = "404", description = "No CIP-68 reference NFT found for this policy/asset")
            })
    @GetMapping(path = "/cip68/ft/{policyId}/{assetName}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<FungibleTokenMetadata> getFungibleTokenMetadata(
            @Parameter(description = "The policy ID (56 hex characters)")
            @PathVariable("policyId") String policyId,
            @Parameter(description = "The reference NFT asset name (hex, with 000643b0 prefix)")
            @PathVariable("assetName") String assetName) {

        return cip68StorageReader.findByPolicyIdAndAssetName(policyId, assetName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(operationId = "getCip68NftMetadata",
            summary = "Get CIP-68 NFT metadata by policy ID and asset name (not yet implemented)",
            responses = {
                    @ApiResponse(responseCode = "501", description = "NFT metadata support is not yet implemented")
            })
    @GetMapping(path = "/cip68/nft/{policyId}/{assetName}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<Void> getNftMetadata(
            @Parameter(description = "The policy ID (56 hex characters)")
            @PathVariable("policyId") String policyId,
            @Parameter(description = "The NFT asset name (hex)")
            @PathVariable("assetName") String assetName) {

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
