package com.bloxbean.cardano.yaci.store.assets.storage.repository;

import com.bloxbean.cardano.yaci.store.assets.storage.model.TxAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TxAssetRepository extends JpaRepository<TxAssetEntity, Long> {
    int deleteBySlotGreaterThan(Long slot);
}
