package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Extension;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

/**
 * The CIP-113 extension of a programmable token: the current node of its policy in the registry (ADR 0011).
 */
@Schema(description = "CIP-113 programmable token metadata. Present when the token's policy ID is "
        + "registered in an on-chain CIP-113 programmable token registry. CIP-113 tokens are standard "
        + "CIP-26/CIP-68 tokens with additional on-chain transfer validation logic — effectively "
        + "tokens in a 'smart contract jail' for regulatory compliance, freeze/seize, or custom rules.")
public record ProgrammableTokenCip113(
        @Schema(description = "Blake2b-224 hash of the minting logic credential (script or verification key) "
                + "that authorises minting and burning of this token. Bound on-chain to the token's policy ID "
                + "at registration. Null when the registry node does not specify one.",
                example = "f462a4e22e5b138c17d893d4e0811790f51f1231b8be4198313e08d0",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("minting_logic_script") @Nullable String mintingLogicScript,
        @Schema(description = "Blake2b-224 hash of the Plutus script that validates every transfer of this token. "
                + "This script runs on-chain via the withdraw-zero pattern whenever tokens move between addresses. "
                + "Null when the registry node does not specify a transfer logic script.",
                example = "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("transfer_logic_script") @Nullable String transferLogicScript,
        @Schema(description = "Blake2b-224 hash of the Plutus script that validates issuer/admin operations "
                + "such as freeze, seize, or burn. Allows a token issuer to perform privileged actions "
                + "on tokens held by other addresses. Null when the substandard has no third-party operations.",
                example = "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("third_party_transfer_logic_script") @Nullable String thirdPartyTransferLogicScript,
        @Schema(description = "Blake2b-224 hash of the credential (script or verification key) that must approve "
                + "any unfracking action touching this token. Null means unfracking is forbidden for this token.",
                example = "f91c65ec585a20176feeb2c4e5281e856d3b61e95d1980717da724fc",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("unfracking_logic_script") @Nullable String unfrackingLogicScript,
        @Schema(description = "Policy ID of an optional global state NFT used by the transfer logic. "
                + "For example, a freeze-and-seize substandard uses this to reference a denylist "
                + "of blocked addresses. Null when the token's substandard has no global state.",
                example = "1234567890abcdef1234567890abcdef1234567890abcdef12345678",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("global_state_policy_id") @Nullable String globalStatePolicyId
) implements Extension {

    /** The key of the extension in the subject's {@code extensions}. */
    public static final String EXTENSION_KEY = "cip113";
}
