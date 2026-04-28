package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Extension;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Schema(description = "CIP-113 programmable token metadata. Present when the token's policy ID is "
        + "registered in an on-chain CIP-113 programmable token registry.")
public record ProgrammableTokenCip113(

        @JsonProperty("transfer_logic_script")
        @Nullable
        @Schema(description = "Blake2b-224 hash of the credential that validates every transfer of this token. "
                + "Verification depends on the companion {@code transfer_logic_script_type}: vkey-credentials "
                + "are checked via signature presence, script-credentials run on-chain via the withdraw-zero "
                + "pattern. Null when the registry node does not specify a transfer logic credential.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String transferLogicScript,

        @JsonProperty("transfer_logic_script_type")
        @Nullable
        @Schema(description = "Aiken Credential discriminator for transfer_logic_script: 'VKEY' = "
                + "VerificationKey hash; 'SCRIPT' = Plutus script hash. Non-null iff transfer_logic_script "
                + "is non-null.",
                nullable = true,
                allowableValues = {"VKEY", "SCRIPT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Cip113CredentialType transferLogicScriptType,

        @JsonProperty("third_party_transfer_logic_script")
        @Nullable
        @Schema(description = "Blake2b-224 hash of the credential for issuer operations (freeze, seize, burn). "
                + "Not all programmable token substandards require this.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String thirdPartyTransferLogicScript,

        @JsonProperty("third_party_transfer_logic_script_type")
        @Nullable
        @Schema(description = "Aiken Credential discriminator for third_party_transfer_logic_script. "
                + "Non-null iff third_party_transfer_logic_script is non-null.",
                nullable = true,
                allowableValues = {"VKEY", "SCRIPT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Cip113CredentialType thirdPartyTransferLogicScriptType,

        @JsonProperty("global_state_policy_id")
        @Nullable
        @Schema(description = "Optional policy ID for global state (e.g. a denylist NFT). Null when absent.",
                nullable = true)
        String globalStatePolicyId

) implements Extension {

    public static final String EXTENSION_KEY = "cip113";
}
