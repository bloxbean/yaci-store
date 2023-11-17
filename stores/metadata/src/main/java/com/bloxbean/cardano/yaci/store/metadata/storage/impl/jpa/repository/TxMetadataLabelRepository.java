package com.bloxbean.cardano.yaci.store.metadata.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.metadata.storage.impl.jpa.model.TxMetadataLabelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TxMetadataLabelRepository extends JpaRepository<TxMetadataLabelEntity, Long> {
    int deleteBySlotGreaterThan(Long slot);
}
