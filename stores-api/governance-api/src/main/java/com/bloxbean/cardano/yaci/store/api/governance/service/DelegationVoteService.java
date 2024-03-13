package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.storage.DelegationVoteStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DelegationVoteService {
    private final DelegationVoteStorageReader delegationVoteStorageReader;

    public List<DelegationVote> getDelegations(int page, int count, Order order) {
        return delegationVoteStorageReader.findAll(page, count, order);
    }

    public List<DelegationVote> getDelegationsByDRepId(String dRepId, int page, int count, Order order) {
        return delegationVoteStorageReader.findByDRepId(dRepId, page, count, order);
    }

    public List<DelegationVote> getDelegationsByAddress(String address, int page, int count, Order order) {
        return delegationVoteStorageReader.findByAddress(address, page, count, order);
    }
}
