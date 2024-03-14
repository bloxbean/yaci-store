package com.bloxbean.cardano.yaci.store.account.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressTxAmountEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressTxAmountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressTxAmountRepository extends JpaRepository<AddressTxAmountEntity, AddressTxAmountId> {
    int deleteAddressBalanceBySlotGreaterThan(long slot);
}
