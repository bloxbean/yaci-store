package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalTreasuryWithdrawalEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalTreasuryWithdrawalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalTreasuryWithdrawalRepository extends JpaRepository<LocalTreasuryWithdrawalEntity, LocalTreasuryWithdrawalId> {
    int deleteBySlotGreaterThan(long slot);
}
