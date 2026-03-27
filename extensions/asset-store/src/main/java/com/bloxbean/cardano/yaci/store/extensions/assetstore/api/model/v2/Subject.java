package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "A token subject with its metadata, standards details, and optional extensions from additional CIPs")
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
        Standards standards,

        @Schema(description = "Additional on-chain properties from CIP extensions. "
                + "Each key is a CIP identifier (e.g. 'cip113'). "
                + "Omitted when the token has no extensions.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Extension> extensions) {
}
