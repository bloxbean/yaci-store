package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;
import org.springframework.data.util.Pair;

import java.util.Collection;
import java.util.List;

public interface CommitteeVoteStorage {
    void saveAll(List<CommitteeVote> committeeVotes);
    int deleteBySlotGreaterThan(long slot);
    List<CommitteeVote> findByGovActionIdsWithMaxSlot(Collection<GovActionId> govActionIds);
    List<CommitteeVote> findByGovActionIdAndVoterHashWithMaxSlot(Collection<Pair<GovActionId, String>> govActionIdVoterHashPairs);
}
