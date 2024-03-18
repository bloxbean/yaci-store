package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.config;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.JpaBlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.JpaBlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.JpaRollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.JpaBlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.JpaBlockRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.JpaRollbackRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JpaConfig implements StorageConfig {

    private final JpaBlockRepository jpaBlockRepository;
    private final JpaBlockMapper jpaBlockMapper;
    private final JpaRollbackRepository jpaRollbackRepository;

    public BlockStorage blockStorage() {
        return new JpaBlockStorage(jpaBlockRepository, jpaBlockMapper);
    }

    @Override
    public BlockStorageReader blockStorageReader() {
        return new JpaBlockStorageReader(jpaBlockRepository, jpaBlockMapper);
    }

    @Override
    public RollbackStorage rollbackStorage() {
        return new JpaRollbackStorage(jpaRollbackRepository);
    }
}
