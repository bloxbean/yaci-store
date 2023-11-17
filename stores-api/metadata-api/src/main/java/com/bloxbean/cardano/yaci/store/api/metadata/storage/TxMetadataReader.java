package com.bloxbean.cardano.yaci.store.api.metadata.storage;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;

import java.util.List;

public interface TxMetadataReader {
    List<TxMetadataLabel> findByTxHash(String txHash);
    List<TxMetadataLabel> findByLabel(String label, int page, int count);
}
