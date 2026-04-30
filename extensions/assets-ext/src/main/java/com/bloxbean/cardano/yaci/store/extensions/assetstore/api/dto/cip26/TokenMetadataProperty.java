package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic CIP-26 signed-property envelope: a property {@code value} plus its detached
 * {@code signatures} and a monotonic {@code sequenceNumber}.
 * <p>
 * Ported from {@code cf-token-metadata-registry} ({@code TokenMetadataProperty}) so that
 * yaci-store's {@code standards.cip26} block matches the CF V2 wire shape exactly. Yaci
 * persists the full envelope in {@code ft_offchain_metadata.properties} (a JSON column),
 * so populating these fields is just a copy from {@link com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenMetadataProperty<T> {

    @JsonProperty("signatures")
    @Valid
    @Schema(name = "signatures", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AnnotatedSignature> signatures = new ArrayList<>();

    @JsonProperty("sequenceNumber")
    @Valid
    @DecimalMin("0")
    @Schema(name = "sequenceNumber", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal sequenceNumber;

    @JsonProperty("value")
    private T value;
}
