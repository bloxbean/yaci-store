package com.bloxbean.cardano.yaci.store.core.storage.impl.redis.config;

import com.bloxbean.cardano.yaci.store.core.storage.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.RedisCursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.RedisEraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.mapper.RedisEraMapper;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.repository.RedisCursorRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.repository.RedisEraRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RedisConfig implements StorageConfig {

    private final RedisCursorRepository redisCursorRepository;
    private final RedisEraRepository redisEraRepository;
    private final RedisEraMapper redisEraMapper;

    @Override
    public CursorStorage cursorStorage() {
        return new RedisCursorStorage(redisCursorRepository);
    }

    @Override
    public EraStorage eraStorage() {
        return new RedisEraStorage(redisEraRepository, redisEraMapper);
    }
}
