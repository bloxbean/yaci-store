package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token type classification based on on-chain constraints.")
public enum TokenType {
    @Schema(description = "Standard Cardano token with no on-chain transfer logic.")
    NATIVE,

    @Schema(description = "Token with on-chain transfer validation rules (e.g. CIP-113).")
    PROGRAMMABLE
}
