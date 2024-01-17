package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;

import java.util.List;

public interface DelegationVoteStorage {
    void saveAll(List<DelegationVote> delegationVotes);

    int deleteBySlotGreaterThan(long slot);
}
