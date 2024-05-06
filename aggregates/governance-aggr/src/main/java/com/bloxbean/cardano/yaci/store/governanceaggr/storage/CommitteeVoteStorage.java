package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;

import java.util.List;

public interface CommitteeVoteStorage {
    void saveAll(List<CommitteeVote> committeeVotes);
}
