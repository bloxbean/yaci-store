package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Schema(description = "Summary of a token under a policy with basic display metadata.")
public record PolicyTokenSummary(
        @Schema(description = "The subject identifier (policyId + assetName hex).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String subject,

        @Nullable
        @Schema(description = "Human-readable token name.")
        String name,

        @Nullable
        @Schema(description = "Token ticker symbol.")
        String ticker,

        @Nullable
        @Schema(description = "Number of decimal places.")
        Long decimals,

        @Schema(description = "Which CIP standard provided this metadata.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String source
) {}
