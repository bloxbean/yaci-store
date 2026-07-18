package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.TokenMetadataProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TickerProperty extends TokenMetadataProperty<String> {
    @Valid
    @Size(min = 2, max = 9)
    @Schema(name = "value", example = "QUID", requiredMode = Schema.RequiredMode.REQUIRED)
    @Override
    public String getValue() {
        return super.getValue();
    }
}
