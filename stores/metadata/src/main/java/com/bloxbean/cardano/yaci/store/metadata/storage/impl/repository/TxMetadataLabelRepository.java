package com.bloxbean.cardano.yaci.store.metadata.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.metadata.storage.impl.model.TxMetadataLabelEntityJpa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxMetadataLabelRepository extends JpaRepository<TxMetadataLabelEntityJpa, Long> {
    int deleteBySlotGreaterThan(Long slot);

    //Optional read queries
    List<TxMetadataLabelEntityJpa> findByTxHash(String txHash);
    Slice<TxMetadataLabelEntityJpa> findByLabel(String label, Pageable pageable);
}
