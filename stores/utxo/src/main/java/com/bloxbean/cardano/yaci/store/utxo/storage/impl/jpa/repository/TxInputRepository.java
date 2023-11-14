package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.TxInputEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.UtxoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TxInputRepository extends JpaRepository<TxInputEntity, UtxoId> {
    int deleteBySpentAtSlotGreaterThan(Long slot);
}

