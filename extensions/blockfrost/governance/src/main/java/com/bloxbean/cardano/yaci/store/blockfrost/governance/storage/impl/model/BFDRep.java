package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated model for DRep list and detail responses.
 * Combines DRep state with voting power, credential type, and anchor data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BFDRep {
    private String drepId;
    private String drepHash;
    private String status;
    /** Epoch of most recent DRep activity (last update/vote) */
    private Integer epoch;
    /** Epoch of first DRep registration */
    private Integer activeEpoch;
    /** Voting power from local_drep_dist (null if adapot disabled) */
    private Long amount;
    /** True if credential type is SCRIPTHASH */
    private Boolean hasScript;
    /** True if DRep has been inactive for more than drep_activity epochs */
    private boolean expired;
    /** URL from the most recent registration or update carrying an anchor */
    private String anchorUrl;
    /** Hash from the most recent registration or update carrying an anchor */
    private String anchorHash;
}
