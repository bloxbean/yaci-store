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
public class BFProposalVoteDto {
    private String txHash;
    private Integer certIndex;
    /** Values: "constitutional_committee", "drep", "spo" */
    private String voterRole;
    /** bech32 for drep/spo, hex for CC */
    private String voter;
    /** Values: "yes", "no", "abstain" */
    private String vote;
}
