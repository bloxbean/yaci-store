package com.bloxbean.cardano.yaci.store.api.metadata.service;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataService {
    private final TxMetadataStorageReader metadataReader;

    public List<TxMetadataLabel> getMetadataForTx(String txHash) {
        return metadataReader.findByTxHash(txHash);
    }

    public List<TxMetadataLabel> getMetadataByLabel(String label, int page, int count) {
        return metadataReader.findByLabel(label, page, count);
    }
}
