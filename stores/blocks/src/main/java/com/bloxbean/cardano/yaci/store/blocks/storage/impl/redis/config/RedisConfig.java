package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.config;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.RedisBlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.RedisBlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.RedisRollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.mapper.RedisBlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.RedisBlockRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.RedisRollbackRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RedisConfig implements StorageConfig {

    private final RedisBlockRepository redisBlockRepository;
    private final RedisBlockMapper redisBlockMapper;
    private final RedisRollbackRepository redisRollbackRepository;

    @Override
    public BlockStorage blockStorage() {
        return new RedisBlockStorage(redisBlockRepository, redisBlockMapper);
    }

    @Override
    public BlockStorageReader blockStorageReader() {
        return new RedisBlockStorageReader(redisBlockRepository, redisBlockMapper);
    }

    @Override
    public RollbackStorage rollbackStorage() {
        return new RedisRollbackStorage(redisRollbackRepository);
    }
}
