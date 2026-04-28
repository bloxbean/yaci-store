package com.bloxbean.cardano.yaci.store.blockfrost.governance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class BFProposalDto {
    /** CIP-129 bech32 governance action id */
    private String id;
    private String txHash;
    private Integer certIndex;
    private String governanceType;
    private JsonNode governanceDescription;
    private String deposit;
    private String returnAddress;
    private Integer ratifiedEpoch;
    private Integer enactedEpoch;
    private Integer droppedEpoch;
    private Integer expiredEpoch;
    private Integer expiration;
}
