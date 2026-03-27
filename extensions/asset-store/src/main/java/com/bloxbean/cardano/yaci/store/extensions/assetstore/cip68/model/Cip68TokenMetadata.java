package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Schema(description = "CIP-68 token metadata parsed from a reference NFT inline datum. "
        + "Applies to both fungible and non-fungible tokens that follow the CIP-68 standard.")
public record Cip68TokenMetadata(
        @Nullable @Schema(description = "Number of decimal places.") Long decimals,
        @Nullable @Schema(description = "Token description.") String description,
        @Nullable @Schema(description = "Base64-encoded PNG logo.") String logo,
        @Nullable @Schema(description = "Human-readable token name.") String name,
        @Nullable @Schema(description = "Token ticker symbol.") String ticker,
        @Nullable @Schema(description = "Project URL.") String url,
        @Nullable @Schema(description = "CIP-68 metadata version.") Long version
) {}
