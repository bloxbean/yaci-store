package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;

import java.util.List;

public interface VotingProcedureStorageReader {

    List<VotingProcedure> findAll(int page, int count, Order order);

    List<VotingProcedure> findByTxHash(String txHash);

    List<VotingProcedure> findByGovActionTxHash(String govActionTxHash, int page, int count);

    List<VotingProcedure> findByGovActionTxHashAndGovActionIndex(String govActionTxHash, int index);
}
