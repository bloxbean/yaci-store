package com.bloxbean.cardano.yaci.store.adminui.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KoiosTotalsDto {
    @JsonProperty("epoch_no")
    private int epochNo;
    private String treasury;
    private String reserves;
}
