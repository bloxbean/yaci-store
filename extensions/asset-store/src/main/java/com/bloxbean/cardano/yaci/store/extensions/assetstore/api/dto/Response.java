package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Single subject query response with the resolved query priority.")
public record Response(
        @Schema(description = "The merged subject with metadata and extensions.")
        Subject subject,

        @Schema(description = "The CIP priority order that was used for this query.")
        List<String> queryPriority
) {}
