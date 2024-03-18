package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaTxInputEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaUtxoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaTxInputRepository extends JpaRepository<JpaTxInputEntity, JpaUtxoId> {
    int deleteBySpentAtSlotGreaterThan(Long slot);
}

