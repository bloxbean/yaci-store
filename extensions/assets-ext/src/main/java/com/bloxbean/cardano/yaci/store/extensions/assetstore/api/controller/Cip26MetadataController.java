package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix:/api/v1}")
@RequiredArgsConstructor
@Tag(name = "CIP-26 Metadata", description = "Off-chain fungible token metadata from the GitHub cardano-token-registry")
@Slf4j
public class Cip26MetadataController {

    private final Cip26StorageReader cip26StorageReader;

    @Operation(operationId = "getCip26MetadataBySubject",
            summary = "Get CIP-26 off-chain metadata by subject",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Metadata found"),
                    @ApiResponse(responseCode = "404", description = "Subject not found in CIP-26 registry")
            })
    @GetMapping(path = "/cip26/{subject}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<TokenMetadata> getMetadataBySubject(
            @Parameter(description = "The subject identifier (policy ID + asset name hex)")
            @PathVariable("subject") String subject) {

        return cip26StorageReader.findBySubject(subject)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(operationId = "getCip26MetadataByPolicyAndAssetName",
            summary = "Get CIP-26 off-chain metadata by policy ID and asset name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Metadata found"),
                    @ApiResponse(responseCode = "404", description = "Not found in CIP-26 registry")
            })
    @GetMapping(path = "/cip26/{policyId}/{assetName}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<TokenMetadata> getMetadataByPolicyAndAssetName(
            @Parameter(description = "The policy ID (56 hex characters)")
            @PathVariable("policyId") String policyId,
            @Parameter(description = "The asset name (hex-encoded)")
            @PathVariable("assetName") String assetName) {

        return cip26StorageReader.findBySubject(policyId + assetName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
