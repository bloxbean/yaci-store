package com.bloxbean.cardano.yaci.store.blocks.storage.impl;

import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockCbor;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockCborEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockCborRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockRepository;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class BlockStorageImpl implements BlockStorage {
    private final static String PLUGIN_BLOCK_SAVE = "block.save";
    private final BlockRepository blockRepository;
    private final BlockCborRepository blockCborRepository;
    private final BlockMapper blockDetailsMapper;
    private final BlocksStoreProperties blocksStoreProperties;

    @Override
    public Optional<Block> findRecentBlock() {
        return blockRepository.findTopByOrderByNumberDesc()
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity));
    }

    @Override
    public List<Block> findBlocksByEpoch(int epochNumber) {
        return blockRepository.findByEpochNumber(epochNumber)
                .stream()
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int deleteBySlotGreaterThan(long slot) {
        // Delete CBOR data first
        blockCborRepository.deleteBySlotGreaterThan(slot);
        
        // Delete block data
        return blockRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Plugin(key = PLUGIN_BLOCK_SAVE)
    @Transactional
    public void save(Block block) {
        // Save parsed block data
        BlockEntity blockEntity = blockDetailsMapper.toBlockEntity(block);
        blockRepository.save(blockEntity);
    }
    
    @Override
    public void saveCbor(BlockCbor blockCbor) {
        if (blockCbor.getCborData() != null && blockCbor.getCborData().length > 0) {
            BlockCborEntity cborEntity = BlockCborEntity.builder()
                    .blockHash(blockCbor.getBlockHash())
                    .cborData(blockCbor.getCborData())
                    .cborSize(blockCbor.getCborSize())
                    .slot(blockCbor.getSlot())
                    .build();
            
            blockCborRepository.save(cborEntity);
            log.debug("Saved CBOR data for block {}", blockCbor.getBlockHash());
        }
    }
}
