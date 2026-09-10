package com.bloxbean.cardano.yaci.store.blockfrost.network.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFEraDto {

    private EraBoundary start;
    private EraBoundary end;
    private EraParameters parameters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class EraBoundary {
       
        private Long time;
        private Long slot;
        private Integer epoch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class EraParameters {
    
        private Long epochLength;
        private Integer slotLength;
        private Long safeZone;
    }
}
