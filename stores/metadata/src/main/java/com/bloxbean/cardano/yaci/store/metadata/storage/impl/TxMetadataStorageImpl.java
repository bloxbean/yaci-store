package com.bloxbean.cardano.yaci.store.metadata.storage.impl;

import com.bloxbean.cardano.yaci.store.common.util.UUIDUtil;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorage;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.mapper.MetadataMapper;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.model.TxMetadataLabelEntity;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.repository.TxMetadataLabelRepository;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.metadata.jooq.Tables.TRANSACTION_METADATA;

@RequiredArgsConstructor
public class TxMetadataStorageImpl implements TxMetadataStorage {
    private final static String PLUGIN_METADATA_SAVE = "metadata.save";

    private final TxMetadataLabelRepository metadataLabelRepository;
    private final MetadataMapper metadataMapper;
    private final DSLContext dsl;

    @Override
    @Plugin(key = PLUGIN_METADATA_SAVE)
    public List<TxMetadataLabel> saveAll(@NonNull List<TxMetadataLabel> txMetadataLabelList) {
        List<TxMetadataLabelEntity> txMetadataLabelEntities = txMetadataLabelList.stream()
                .map(metadataMapper::toTxMetadataLabelEntity)
                .toList();

        //Id from (tx_hash, label), so a block processed again overwrites its rows
        var idField = UUIDUtil.idField(dsl, TRANSACTION_METADATA.ID);
        LocalDateTime localDateTime = LocalDateTime.now();
        var inserts = txMetadataLabelEntities.stream()
                .map(entity -> dsl.insertInto(TRANSACTION_METADATA)
                        .set(idField, UUIDUtil.nameUUID(entity.getTxHash(), entity.getLabel()))
                        .set(TRANSACTION_METADATA.SLOT, entity.getSlot())
                        .set(TRANSACTION_METADATA.TX_HASH, entity.getTxHash())
                        .set(TRANSACTION_METADATA.TX_INDEX, entity.getTxIndex())
                        .set(TRANSACTION_METADATA.LABEL, entity.getLabel())
                        .set(TRANSACTION_METADATA.BODY, entity.getBody())
                        .set(TRANSACTION_METADATA.CBOR, entity.getCbor())
                        .set(TRANSACTION_METADATA.BLOCK, entity.getBlockNumber())
                        .set(TRANSACTION_METADATA.BLOCK_TIME, entity.getBlockTime())
                        .set(TRANSACTION_METADATA.UPDATE_DATETIME, localDateTime)
                        .onDuplicateKeyUpdate()
                        .set(TRANSACTION_METADATA.SLOT, entity.getSlot())
                        .set(TRANSACTION_METADATA.TX_HASH, entity.getTxHash())
                        .set(TRANSACTION_METADATA.TX_INDEX, entity.getTxIndex())
                        .set(TRANSACTION_METADATA.LABEL, entity.getLabel())
                        .set(TRANSACTION_METADATA.BODY, entity.getBody())
                        .set(TRANSACTION_METADATA.CBOR, entity.getCbor())
                        .set(TRANSACTION_METADATA.BLOCK, entity.getBlockNumber())
                        .set(TRANSACTION_METADATA.BLOCK_TIME, entity.getBlockTime())
                        .set(TRANSACTION_METADATA.UPDATE_DATETIME, localDateTime)).toList();

        dsl.batch(inserts).execute();
        return txMetadataLabelList;
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return metadataLabelRepository.deleteBySlotGreaterThan(slot);
    }

}
