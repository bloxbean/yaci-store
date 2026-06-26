package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.TokenMetadataProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Logo property — value is a base64-encoded PNG, per CIP-26 spec.
 * <p>
 * Differs from CF's {@code LogoProperty extends TokenMetadataProperty<byte[]>}: yaci uses
 * {@code String} so the already-base64 string from the registry passes through unchanged.
 * Wire shape is identical (Jackson serializes both as a base64 text node), so consumers
 * see the same JSON. CF returns hex-encoded bytes today; yaci stays spec-compliant on
 * base64. The user explicitly accepted that as the only divergence.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LogoProperty extends TokenMetadataProperty<String> {
    @Schema(name = "value", description = "base64-encoded PNG", requiredMode = Schema.RequiredMode.REQUIRED)
    @Override
    public String getValue() {
        return super.getValue();
    }
}
