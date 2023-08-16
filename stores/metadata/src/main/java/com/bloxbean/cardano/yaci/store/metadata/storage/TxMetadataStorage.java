package com.bloxbean.cardano.yaci.store.metadata.storage;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;

import java.util.List;

public interface TxMetadataStorage {
    List<TxMetadataLabel> saveAll(List<TxMetadataLabel> txMetadataLabelList);
    List<TxMetadataLabel> findByTxHash(String txHash);
    List<TxMetadataLabel> findByLabel(String label, int page, int count);

    int deleteBySlotGreaterThan(long slot);
}
