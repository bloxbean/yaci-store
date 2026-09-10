package com.bloxbean.cardano.yaci.store.blockfrost.governance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class BFDRepVoteDto {
    private String txHash;
    private Integer certIndex;
    /** CIP-129 bech32 governance action id */
    private String proposalId;
    private String proposalTxHash;
    private Integer proposalCertIndex;
    /** Values: "yes", "no", "abstain" */
    private String vote;
}
