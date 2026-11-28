package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token type classification. PROGRAMMABLE indicates the token has on-chain transfer logic (e.g. CIP-113).")
public enum TokenType {
    NATIVE,
    PROGRAMMABLE
}
