package com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.AmountId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.AmtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmtRepository extends JpaRepository<AmtEntity, AmountId> {
    int deleteBySlotGreaterThan(Long slot);
}

