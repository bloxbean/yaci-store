package com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.WithdrawalEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.WithdrawalId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<WithdrawalEntity, WithdrawalId> {

    List<WithdrawalEntity> findByTxHash(String txHash);

    @Query("select w from WithdrawalEntity w")
    Slice<WithdrawalEntity> findAllWithdrawals(Pageable pageable);

    Slice<WithdrawalEntity> findByAddress(String address, Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
