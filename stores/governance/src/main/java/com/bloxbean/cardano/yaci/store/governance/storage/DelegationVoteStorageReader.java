package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;

import java.util.List;

public interface DelegationVoteStorageReader {
    List<DelegationVote> findAll(int page, int count);

    List<DelegationVote> findByDRepId(String dRepId);
}
