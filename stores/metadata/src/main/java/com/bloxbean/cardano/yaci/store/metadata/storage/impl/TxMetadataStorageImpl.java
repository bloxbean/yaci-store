package com.bloxbean.cardano.yaci.store.metadata.storage.impl;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorage;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.mapper.MetadataMapper;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.model.TxMetadataLabelEntity;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.repository.TxMetadataLabelRepository;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TxMetadataStorageImpl implements TxMetadataStorage {
    private final static String FILTER_METADATA_SAVE = "metadata.save";

    private final TxMetadataLabelRepository metadataLabelRepository;
    private final MetadataMapper metadataMapper;

    @Override
    @Plugin(key = FILTER_METADATA_SAVE)
    public List<TxMetadataLabel> saveAll(@NonNull List<TxMetadataLabel> txMetadataLabelList) {
        List<TxMetadataLabelEntity> txMetadataLabelEntities = txMetadataLabelList.stream()
                .map(metadataMapper::toTxMetadataLabelEntity)
                .toList();

        List<TxMetadataLabelEntity> savedEntities = metadataLabelRepository.saveAll(txMetadataLabelEntities);
        return savedEntities.stream()
                .map(metadataMapper::toTxMetadataLabel)
                .toList();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return metadataLabelRepository.deleteBySlotGreaterThan(slot);
    }

}
