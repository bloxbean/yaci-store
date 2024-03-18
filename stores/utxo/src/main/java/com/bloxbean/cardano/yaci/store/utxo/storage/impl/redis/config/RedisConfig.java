package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.config;

import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.RedisUtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.RedisUtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.repository.RedisTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.repository.RedisUtxoRepository;
import com.redis.om.spring.search.stream.EntityStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RedisConfig implements StorageConfig {

    private final RedisUtxoRepository redisUtxoRepository;
    private final RedisTxInputRepository redisTxInputRepository;
    private final EntityStream entityStream;
    private final UtxoCache utxoCache;

    @Override
    public UtxoStorage utxoStorage() {
        return new RedisUtxoStorage(redisUtxoRepository, redisTxInputRepository, utxoCache);
    }

    @Override
    public UtxoStorageReader utxoStorageReader() {
        return new RedisUtxoStorageReader(redisUtxoRepository, redisTxInputRepository, entityStream);
    }
}
