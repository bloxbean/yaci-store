package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2.Extension;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Schema(description = "CIP-113 programmable token metadata. Present when the token's policy ID is "
        + "registered in an on-chain CIP-113 programmable token registry. CIP-113 tokens are standard "
        + "CIP-26/CIP-68 tokens with additional on-chain transfer validation logic -- effectively "
        + "tokens in a 'smart contract jail' for regulatory compliance, freeze/seize, or custom rules.")
public record ProgrammableTokenCip113(

        @JsonProperty("transfer_logic_script") String transferLogicScript,

        @JsonProperty("third_party_transfer_logic_script") String thirdPartyTransferLogicScript,

        @JsonProperty("global_state_policy_id") @Nullable String globalStatePolicyId

) implements Extension {

    public static final String EXTENSION_KEY = "cip113";

}
