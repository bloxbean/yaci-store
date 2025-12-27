package com.bloxbean.cardano.yaci.store.governance.domain;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import com.bloxbean.cardano.yaci.store.common.util.GovUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VotingProcedure extends BlockAwareDomain {
    private UUID id;

    private String txHash;

    private long index;

    private int txIndex;

    private Long slot;

    private VoterType voterType;

    private String voterHash;

    private String govActionTxHash;

    private Integer govActionIndex;

    private Vote vote;

    private String anchorUrl;

    private String anchorHash;

    private Integer epoch;

    /**
     * Get CIP-129 compliant governance action ID in bech32 format for the governance action being voted on.
     * This is a computed field derived from govActionTxHash and govActionIndex.
     * @return The governance action ID (e.g., "gov_action1..."), or null if govActionTxHash or govActionIndex is null
     */
    public String getGovActionId() {
        if (govActionTxHash == null || govActionIndex == null) {
            return null;
        }
        return GovUtil.toGovActionIdBech32(govActionTxHash, govActionIndex);
    }
}
