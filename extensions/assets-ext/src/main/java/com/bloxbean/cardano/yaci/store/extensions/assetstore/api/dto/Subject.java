package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.util.List;

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
        @Nullable Standards standards,

        @JsonProperty("validation_errors")
        @Schema(name = "validation_errors",
                description = "How the metadata of the token departs from its CIP, from every standard that has it (each entry "
                        + "names its cip and, for CIP-68, its sub_standard, the label). The token is returned as indexed; a MANDATORY "
                        + "error means it lacks something its CIP requires, so a wallet that wants to be strict may reject it.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ValidationError> validationErrors) {

    public Subject {
        validationErrors = validationErrors == null ? List.of() : validationErrors;
    }

    /** A subject with no validation errors. */
    public Subject(String subject, Metadata metadata, @Nullable Standards standards) {
        this(subject, metadata, standards, List.of());
    }
}
