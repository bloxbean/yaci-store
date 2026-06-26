package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix}/tokens")
@RequiredArgsConstructor
@Tag(name = "CIP-68 Metadata", description = "On-chain fungible token metadata parsed from CIP-68 reference NFT inline datums")
@Slf4j
public class Cip68MetadataController {

    private final Cip68StorageReader cip68StorageReader;

    @Operation(operationId = "getCip68FungibleTokenMetadataBySubject",
            summary = "Get CIP-68 fungible token metadata by subject",
            description = "The subject is the fungible token unit (policyId + 0014df10 + hex name). "
                    + "The CIP-68 reference NFT prefix is resolved automatically.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fungible token metadata found"),
                    @ApiResponse(responseCode = "404", description = "No CIP-68 reference NFT found for this subject")
            })
    @GetMapping(path = "/cip68/ft/{subject}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<FungibleTokenMetadata> getFungibleTokenMetadataBySubject(
            @Parameter(description = "The fungible token subject (policyId + 0014df10 + hex asset name)")
            @PathVariable("subject")
            @Pattern(regexp = TokenPatterns.SUBJECT_REGEX,
                    message = "subject must be 56-120 hex characters (policyId + assetName)")
            String subject) {

        return cip68StorageReader.findBySubject(subject)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(operationId = "getCip68FungibleTokenMetadata",
            summary = "Get CIP-68 fungible token metadata by policy ID and asset name",
            description = "The asset name is the raw token name (hex) WITHOUT the CIP-68 label prefix. "
                    + "The reference NFT prefix (000643b0) is prepended automatically.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fungible token metadata found"),
                    @ApiResponse(responseCode = "404", description = "No CIP-68 reference NFT found for this policy/asset")
            })
    @GetMapping(path = "/cip68/ft/{policyId}/{rawAssetName}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<FungibleTokenMetadata> getFungibleTokenMetadata(
            @Parameter(description = "The policy ID (56 hex characters)")
            @PathVariable("policyId")
            @Pattern(regexp = TokenPatterns.POLICY_ID_REGEX,
                    message = "policyId must be exactly 56 hex characters")
            String policyId,
            @Parameter(description = "The raw asset name (hex) without CIP-68 label prefix")
            @PathVariable("rawAssetName")
            @Pattern(regexp = TokenPatterns.CIP68_RAW_ASSET_NAME_REGEX,
                    message = "rawAssetName must be 0-56 hex characters (CIP-68 label is prepended automatically)")
            String rawAssetName) {

        String referenceNftAssetName = Cip68Constants.REFERENCE_TOKEN_PREFIX + rawAssetName;
        return cip68StorageReader.findByPolicyIdAndAssetName(policyId, referenceNftAssetName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
