package com.bloxbean.cardano.yaci.store.blockfrost.asset.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFAssetDetailDTO {
    private String asset;
    private String policyId;
    private String assetName;
    private String fingerprint;
    private String quantity;
    private String initialMintTxHash;
    private Long mintOrBurnCount;
    private Object onchainMetadata;
    private String onchainMetadataStandard;
    private String onchainMetadataExtra;
    private Metadata metadata;

    @Getter
    @Setter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Metadata {
        private String name;
        private String description;
        private String ticker;
        private String url;
        private String logo;
        private Integer decimals;
    }
}
