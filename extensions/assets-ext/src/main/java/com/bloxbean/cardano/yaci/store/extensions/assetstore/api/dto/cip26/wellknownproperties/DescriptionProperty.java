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
public class DescriptionProperty extends TokenMetadataProperty<String> {
    @Valid
    @Size(max = 500)
    @Schema(name = "value", requiredMode = Schema.RequiredMode.REQUIRED)
    @Override
    public String getValue() {
        return super.getValue();
    }
}
