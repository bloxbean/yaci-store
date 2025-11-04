package com.bloxbean.cardano.yaci.store.blocks.storage.impl;

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
    public Optional<byte[]> getBlockCborByHash(String blockHash) {
        return blockCborRepository.findByBlockHash(blockHash)
                .map(entity -> entity.getCborData());
    }

    @Override
    public boolean cborExists(String blockHash) {
        return blockCborRepository.findByBlockHash(blockHash).isPresent();
    }
}


