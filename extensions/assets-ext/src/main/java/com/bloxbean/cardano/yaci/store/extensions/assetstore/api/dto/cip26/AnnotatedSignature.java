package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detached signature over a single CIP-26 property value.
 * <p>
 * Ported from {@code cf-token-metadata-registry} ({@code AnnotatedSignature}) so that
 * yaci-store's {@code standards.cip26} block matches the CF V2 wire shape exactly when
 * {@code show_cips_details=true}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotatedSignature {

    @JsonProperty("signature")
    @Schema(name = "signature", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signature;

    @JsonProperty("publicKey")
    @Schema(name = "publicKey", requiredMode = Schema.RequiredMode.REQUIRED)
    private String publicKey;
}
