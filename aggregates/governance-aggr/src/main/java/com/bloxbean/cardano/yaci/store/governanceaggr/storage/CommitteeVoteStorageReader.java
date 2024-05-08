package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;
import org.springframework.data.util.Pair;

import java.util.Collection;
import java.util.List;

public interface CommitteeVoteStorageReader {
    List<CommitteeVote> findByGovActionIdsWithMaxSlot(Collection<GovActionId> govActionIds);
    List<CommitteeVote> findByGovActionIdAndVoterHashWithMaxSlot(Collection<Pair<GovActionId, String>> govActionIdVoterHashPairs);
}
