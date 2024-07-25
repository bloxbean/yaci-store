package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VotingProcedureStorageReader {

    List<VotingProcedure> findAll(int page, int count, Order order);

    Optional<VotingProcedure> findById(UUID id);

    List<VotingProcedure> findByTxHash(String txHash);

    List<VotingProcedure> findByGovActionTxHash(String govActionTxHash, int page, int count, Order order);

    List<VotingProcedure> findByGovActionTxHashAndGovActionIndex(String govActionTxHash, int index, int page, int count, Order order);
    Slice<VotingProcedure> findBySlotGreaterThan(Long slot, Pageable pageable);
}
