package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
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
@Tag(name = "CIP-26 Metadata", description = "Off-chain token metadata (fungible and non-fungible) from the GitHub cardano-token-registry")
@Slf4j
public class Cip26MetadataController {

    private final Cip26StorageReader cip26StorageReader;

    @Operation(operationId = "getCip26Metadata",
            summary = "Get CIP-26 off-chain metadata for a subject",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Metadata found"),
                    @ApiResponse(responseCode = "404", description = "Subject not found in CIP-26 registry")
            })
    @GetMapping(path = "/metadata/{subject}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<TokenMetadata> getMetadata(
            @Parameter(description = "The subject identifier (policy ID + asset name hex)")
            @PathVariable("subject") String subject) {

        return cip26StorageReader.findBySubject(subject)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
