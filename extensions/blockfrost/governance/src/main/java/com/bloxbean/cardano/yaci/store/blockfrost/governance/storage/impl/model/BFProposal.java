package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated model for a governance proposal.
 * Combines gov_action_proposal with status epochs from local_gov_action_proposal_status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BFProposal {
    private String txHash;
    private Integer certIndex;
    private String type;
    private JsonNode details;
    private Long deposit;
    private String returnAddress;
    private Integer epoch;
    /** Epoch when the proposal was ratified (null if not ratified) */
    private Integer ratifiedEpoch;
    /** Epoch when the proposal was expired (null if not expired) */
    private Integer expiredEpoch;
    /** gov_action_lifetime from protocol params (for expiration calculation) */
    private Integer govActionLifetime;
    /** Proposal anchor URL (for metadata endpoint) */
    private String anchorUrl;
    /** Proposal anchor hash (for metadata endpoint) */
    private String anchorHash;
}
