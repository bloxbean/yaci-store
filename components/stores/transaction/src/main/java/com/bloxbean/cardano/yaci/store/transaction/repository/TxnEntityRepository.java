package com.bloxbean.cardano.yaci.store.transaction.repository;

import com.bloxbean.cardano.yaci.store.transaction.model.TxnEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TxnEntityRepository extends PagingAndSortingRepository<TxnEntity, String> {

    Optional<TxnEntity> findByTxHash(String txHash);

    int deleteBySlotGreaterThan(Long slot);
}
