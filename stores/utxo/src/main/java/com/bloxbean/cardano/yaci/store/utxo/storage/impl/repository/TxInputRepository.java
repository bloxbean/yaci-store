package com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.TxInputEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.UtxoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TxInputRepository extends JpaRepository<TxInputEntity, UtxoId> {
    int deleteBySpentAtSlotGreaterThan(Long slot);
}

