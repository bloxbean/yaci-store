package com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.WithdrawalEntityJpa;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.WithdrawalId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<WithdrawalEntityJpa, WithdrawalId> {

    List<WithdrawalEntityJpa> findByTxHash(String txHash);

    @Query("select w from WithdrawalEntityJpa w")
    Slice<WithdrawalEntityJpa> findAllWithdrawals(Pageable pageable);

    Slice<WithdrawalEntityJpa> findByAddress(String address, Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
