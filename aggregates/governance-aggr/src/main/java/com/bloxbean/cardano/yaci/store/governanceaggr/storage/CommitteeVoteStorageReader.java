package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;

import java.util.List;

public interface CommitteeVoteStorageReader {
    List<CommitteeVote> findByGovActionTxHashAndGovActionIndexPairsWithMaxSlot(List<GovActionId> govActionIds);
}
