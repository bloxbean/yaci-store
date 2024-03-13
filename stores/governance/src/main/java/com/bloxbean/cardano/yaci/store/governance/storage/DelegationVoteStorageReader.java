package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;

import java.util.List;

public interface DelegationVoteStorageReader {
    List<DelegationVote> findAll(int page, int count, Order order);

    List<DelegationVote> findByDRepId(String dRepId, int page, int count, Order order);

    List<DelegationVote> findByAddress(String address, int page, int count, Order order);
}
