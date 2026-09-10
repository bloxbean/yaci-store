package com.bloxbean.cardano.yaci.store.metadata.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorageReader;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.mapper.MetadataMapper;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.repository.TxMetadataLabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class TxMetadataStorageReaderImpl implements TxMetadataStorageReader {
    //A slot holds many transactions, so slot alone is not a stable paging order and rows can be
    //dropped or repeated across pages. txHash breaks the tie: a transaction carries at most one
    //metadata entry per label, so (slot, txHash) identifies the row for a given label.
    private static final String[] SORT_PROPERTIES = {"slot", "txHash"};

    private final TxMetadataLabelRepository txMetadataLabelReadRepository;
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
                PageRequest.of(page, count, Sort.by(SORT_PROPERTIES).descending());

        return txMetadataLabelReadRepository.findByLabel(label, sortedBySlot)
                .stream()
                .map(metadataMapper::toTxMetadataLabel)
                .toList();
    }

    @Override
    public List<TxMetadataLabel> findByLabel(String label, int page, int count, Order order) {
        Sort sort = (order == Order.asc)
                ? Sort.by(SORT_PROPERTIES).ascending()
                : Sort.by(SORT_PROPERTIES).descending();
        Pageable pageable = PageRequest.of(page, count, sort);

        return txMetadataLabelReadRepository.findByLabel(label, pageable)
                .stream()
                .map(metadataMapper::toTxMetadataLabel)
                .toList();
    }
}
