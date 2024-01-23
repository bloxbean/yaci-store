package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.storage.DelegationVoteStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DelegationVoteService {
    private final DelegationVoteStorageReader delegationVoteStorageReader;

    public List<DelegationVote> getDelegations(int page, int count) {
        return delegationVoteStorageReader.findAll(page, count);
    }

    public List<DelegationVote> getDelegationsByDRepId(String dRepId) {
        return delegationVoteStorageReader.findByDRepId(dRepId);
    }
}
