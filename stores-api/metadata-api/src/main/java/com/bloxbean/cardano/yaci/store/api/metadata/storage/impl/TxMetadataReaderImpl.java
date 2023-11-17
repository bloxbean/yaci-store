package com.bloxbean.cardano.yaci.store.api.metadata.storage.impl;

import com.bloxbean.cardano.yaci.store.api.metadata.storage.TxMetadataReader;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.jpa.MetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class TxMetadataReaderImpl implements TxMetadataReader {
    private final TxMetadataLabelReadRepository txMetadataLabelReadRepository;
    private final MetadataMapper metadataMapper;

    @Override
    public List<TxMetadataLabel> findByTxHash(String txHash) {
        return txMetadataLabelReadRepository.findByTxHash(txHash)
                .stream()
                .map(metadataMapper::toTxMetadataLabel)
                .toList();
    }

    @Override
    public List<TxMetadataLabel> findByLabel(String label, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return txMetadataLabelReadRepository.findByLabel(label, sortedBySlot)
                .stream()
                .map(metadataMapper::toTxMetadataLabel)
                .toList();
    }
}
