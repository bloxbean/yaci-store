package com.bloxbean.cardano.yaci.store.blocks.storage.impl;

import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockCbor;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockCborStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockCborEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockCborRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class BlockCborStorageImpl implements BlockCborStorage {

    private final BlockCborRepository blockCborRepository;
    private final BlocksStoreProperties blocksStoreProperties;

    @Override
    @Transactional
    public void save(BlockCbor blockCbor) {
        if (!blocksStoreProperties.isSaveCbor()) {
            return;
        }

        if (blockCbor == null || blockCbor.getCborData() == null || blockCbor.getCborData().length == 0) {
            log.debug("Skipping block CBOR save for {} due to empty payload", blockCbor != null ? blockCbor.getBlockHash() : "null");
            return;
        }

        BlockCborEntity cborEntity = BlockCborEntity.builder()
                .blockHash(blockCbor.getBlockHash())
                .cborData(blockCbor.getCborData())
                .cborSize(blockCbor.getCborSize())
                .slot(blockCbor.getSlot())
                .build();

        blockCborRepository.save(cborEntity);
    }

    @Override
    @Transactional
    public int deleteBySlotGreaterThan(long slot) {
        if (!blocksStoreProperties.isSaveCbor()) {
            return 0;
        }
        return blockCborRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Transactional
    public int deleteBySlotLessThan(long slot) {
        if (!blocksStoreProperties.isSaveCbor()) {
            return 0;
        }
        return blockCborRepository.deleteBySlotLessThan(slot);
    }
}
