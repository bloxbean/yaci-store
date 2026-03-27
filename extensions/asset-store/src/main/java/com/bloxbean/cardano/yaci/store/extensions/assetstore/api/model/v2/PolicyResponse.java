package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Policy-level view aggregating all tokens and capabilities for a Cardano minting policy.")
public record PolicyResponse(

        @JsonProperty("policy_id")
        @Schema(description = "The minting policy ID.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String policyId,

        @Schema(description = "Tokens minted under this policy with basic display metadata from CIP-26 and CIP-68.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<PolicyTokenSummary> tokens,

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @Schema(description = "Per-policy extensions keyed by CIP identifier (e.g. 'cip113'). "
                + "Omitted when the policy has no extensions.")
        Map<String, Extension> extensions

) {}
