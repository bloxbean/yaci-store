package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.TokenMetadataProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DecimalsProperty extends TokenMetadataProperty<BigDecimal> {
    @Valid
    @DecimalMin("0")
    @DecimalMax("255")
    @Schema(name = "value", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Override
    public BigDecimal getValue() {
        return super.getValue();
    }
}
