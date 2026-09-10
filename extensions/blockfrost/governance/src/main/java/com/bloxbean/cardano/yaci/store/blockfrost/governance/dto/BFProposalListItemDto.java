package com.bloxbean.cardano.yaci.store.blockfrost.governance.dto;

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
public class BFProposalListItemDto {
    /** CIP-129 bech32 governance action id */
    private String id;
    private String txHash;
    private Integer certIndex;
    private String governanceType;
}
