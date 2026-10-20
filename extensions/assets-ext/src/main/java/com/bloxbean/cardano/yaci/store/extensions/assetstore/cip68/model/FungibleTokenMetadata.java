package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.util.List;

@Schema(description = "CIP-68 fungible (label 333) or rich fungible (label 444) token metadata parsed from a reference NFT inline datum. "
        + "The token is served as indexed, whatever is wrong with it: validation_errors lists how it departs from CIP-68.")
public record FungibleTokenMetadata(
        @Nullable @Schema(description = "Number of decimal places.") Long decimals,
        @Nullable @Schema(description = "Token description.") String description,
        @Nullable @Schema(description = "Logo as written in the datum, a URI with scheme https, ipfs, ar or data when it follows CIP-68. "
                + "For a rich fungible token (444), which has no logo, this is its image.") String logo,
        @Nullable @Schema(description = "Human-readable token name.") String name,
        @Nullable @Schema(description = "Token ticker symbol.") String ticker,
        @Nullable @Schema(description = "Project URL.") String url,
        @Nullable @Schema(description = "CIP-68 metadata version.") Long version,
        @JsonProperty("validation_errors")
        @Schema(name = "validation_errors", description = "How the datum departs from CIP-68; empty when it follows it. "
                + "MANDATORY errors mean the token lacks something CIP-68 requires, so a strict wallet may reject it.")
        List<ValidationError> validationErrors
) {

    public FungibleTokenMetadata {
        validationErrors = validationErrors == null ? List.of() : validationErrors;
    }

    /** Metadata with no validation errors. */
    public FungibleTokenMetadata(@Nullable Long decimals, @Nullable String description, @Nullable String logo,
                                 @Nullable String name, @Nullable String ticker, @Nullable String url,
                                 @Nullable Long version) {
        this(decimals, description, logo, name, ticker, url, version, List.of());
    }
}
