package com.bloxbean.cardano.yaci.store.blocks.storage.impl;

import com.bloxbean.cardano.yaci.store.blocks.domain.BlockCbor;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockCborStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockCborRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class BlockCborStorageReaderImpl implements BlockCborStorageReader {
    private final BlockCborRepository blockCborRepository;

    @Override
    public Optional<BlockCbor> getBlockCborByHash(String blockHash) {
        return blockCborRepository.findByBlockHash(blockHash)
                .map(entity -> BlockCbor.builder()
                        .blockHash(entity.getBlockHash())
                        .cborData(entity.getCborData())
                        .cborSize(entity.getCborSize())
                        .slot(entity.getSlot())
                        .build());
    }

    @Override
    public boolean cborExists(String blockHash) {
        return blockCborRepository.findByBlockHash(blockHash).isPresent();
    }
}


