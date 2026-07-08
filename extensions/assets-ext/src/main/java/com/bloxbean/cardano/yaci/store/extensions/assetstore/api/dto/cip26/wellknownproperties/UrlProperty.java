package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.TokenMetadataProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UrlProperty extends TokenMetadataProperty<String> {
    @Valid
    @Pattern(regexp = "^https://")
    @Size(max = 250)
    @Schema(name = "value", example = "https://www.iohk.io", requiredMode = Schema.RequiredMode.REQUIRED)
    @Override
    public String getValue() {
        return super.getValue();
    }
}
