package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "Batch request for policy lookups.")
public record PolicyBatchRequest(

        @JsonProperty("policy_ids")
        @Valid
        @Schema(description = "List of policy IDs to look up.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> policyIds

) {}
