package com.bloxbean.cardano.yaci.store.api.governanceaggr.dto;

import com.bloxbean.cardano.yaci.store.common.util.GovUtil;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigInteger;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProposalDto {
    private String txHash;

    private int index;

    private Long slot;

    private BigInteger deposit;

    private String returnAddress;

    private JsonNode govAction;

    private String anchorUrl;

    private String anchorHash;

    private ProposalStatus status;

    private ProposalVotingStats votingStats;

    private Integer epoch;

    private Long blockNumber;

    private Long blockTime;

    /**
     * Get CIP-129 compliant governance action ID in bech32 format.
     * This is a computed field derived from txHash and index.
     * @return The governance action ID (e.g., "gov_action1..."), or null if txHash is null
     */
    public String getGovActionId() {
        if (txHash == null) {
            return null;
        }
        return GovUtil.toGovActionIdBech32(txHash, index);
    }
}
