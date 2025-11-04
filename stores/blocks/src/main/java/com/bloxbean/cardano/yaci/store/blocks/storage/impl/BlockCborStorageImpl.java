package com.bloxbean.cardano.yaci.store.blocks.storage.impl;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockCborStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockCborEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockCborRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of BlockCborStorage for managing block CBOR data
 */
@RequiredArgsConstructor
@Slf4j
public class BlockCborStorageImpl implements BlockCborStorage {
    private final BlockCborRepository blockCborRepository;

    @Override
    public void save(Block block) {
        if (block.getBlockCbor() != null && block.getBlockCbor().length > 0) {
            BlockCborEntity cborEntity = BlockCborEntity.builder()
                    .blockHash(block.getHash())
                    .cborData(block.getBlockCbor())
                    .cborSize(block.getBlockCbor().length)
                    .slot(block.getSlot())
                    .build();
            
            blockCborRepository.save(cborEntity);
            log.debug("Saved CBOR data for block {}", block.getHash());
        }
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        int deleted = blockCborRepository.deleteBySlotGreaterThan(slot);
        log.debug("Deleted {} block CBOR records for rollback (slot > {})", deleted, slot);
        return deleted;
    }
}

