package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2.Extension;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Schema(description = "CIP-113 programmable token metadata. Present when the token's policy ID is "
        + "registered in an on-chain CIP-113 programmable token registry.")
public record ProgrammableTokenCip113(

        @JsonProperty("transfer_logic_script")
        @Schema(description = "Blake2b-224 hash of the Plutus script that validates every transfer.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String transferLogicScript,

        @JsonProperty("third_party_transfer_logic_script")
        @Schema(description = "Blake2b-224 hash of the Plutus script for issuer operations (freeze, seize, burn).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String thirdPartyTransferLogicScript,

        @JsonProperty("global_state_policy_id")
        @Nullable
        @Schema(description = "Optional policy ID for global state (e.g. a denylist NFT). Null when absent.",
                nullable = true)
        String globalStatePolicyId

) implements Extension {

    public static final String EXTENSION_KEY = "cip113";
}
