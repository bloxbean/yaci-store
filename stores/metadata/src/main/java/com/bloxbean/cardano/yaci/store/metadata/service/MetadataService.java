package com.bloxbean.cardano.yaci.store.metadata.service;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataService {
    private final TxMetadataStorage metadataStorage;

    public List<TxMetadataLabel> getMetadataForTx(String txHash) {
        return metadataStorage.findByTxHash(txHash);
    }

    public List<TxMetadataLabel> getMetadataByLabel(String label, int page, int count) {
        return metadataStorage.findByLabel(label, page, count);
    }
}
