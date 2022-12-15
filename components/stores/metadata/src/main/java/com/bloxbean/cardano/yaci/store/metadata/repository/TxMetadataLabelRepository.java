package com.bloxbean.cardano.yaci.store.metadata.repository;

import com.bloxbean.cardano.yaci.store.metadata.model.TxMetadataLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxMetadataLabelRepository extends JpaRepository<TxMetadataLabel, Long> {
    List<TxMetadataLabel> findByTxHash(String txHash);

    int deleteBySlotGreaterThan(Long slot);
}
