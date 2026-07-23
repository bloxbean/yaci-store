package com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFTxPoolUpdateDto {
    private Integer certIndex;
    private String poolId;
    private String vrfKey;
    private String pledge;
    private Double marginCost;
    private String fixedCost;
    private String rewardAccount;
    private List<String> owners;
    private PoolMetadata metadata;
    private List<BFTxPoolRelayDto> relays;
    private Integer activeEpoch;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class PoolMetadata {
        private String url;
        private String hash;
        private String ticker;
        private String name;
        private String description;
        private String homepage;
    }
}
