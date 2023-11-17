package com.bloxbean.cardano.yaci.store.api.metadata.storage.impl;

import com.bloxbean.cardano.yaci.store.metadata.storage.impl.jpa.model.TxMetadataLabelEntity;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.jpa.repository.TxMetadataLabelRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxMetadataLabelReadRepository extends TxMetadataLabelRepository {
    List<TxMetadataLabelEntity> findByTxHash(String txHash);
    Slice<TxMetadataLabelEntity> findByLabel(String label, Pageable pageable);
}
