package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TxVote {
    private String txHash;

    private Integer index;

    private Long slot;

    private VoterType voterType;

    private String voterHash;

    private String govActionTxHash;

    private Integer govActionIndex;

    private Vote voteInPrevAggrSlot; // TODO: rename

    private Vote vote;

    private String anchorUrl;

    private String anchorHash;

    private Integer epoch;
}
