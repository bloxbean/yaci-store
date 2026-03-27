package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequest {

    @JsonProperty("subjects")
    @Valid
    @Schema(name = "subjects", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> subjects = new ArrayList<>();

    @JsonProperty("properties")
    @Valid
    @Schema(name = "properties")
    private List<String> properties = null;
}
