package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
    @Size(min = 1, max = 100, message = "subjects list must contain between 1 and 100 entries")
    @Schema(name = "subjects", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> subjects = new ArrayList<>();

    @JsonProperty("properties")
    @Valid
    @Schema(name = "properties")
    private List<String> properties = null;
}
