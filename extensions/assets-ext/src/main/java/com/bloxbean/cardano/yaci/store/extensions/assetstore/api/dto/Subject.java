package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A token subject with its metadata and standards details")
public record Subject(

        @Schema(description = "The subject identifier -- concatenation of policy ID and asset name (hex)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String subject,

        @Schema(description = "Merged display metadata from CIP-26/CIP-68 based on query priority",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Metadata metadata,

        @Schema(description = "Raw per-standard metadata, only present when show_cips_details=true",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Standards standards) {
}
