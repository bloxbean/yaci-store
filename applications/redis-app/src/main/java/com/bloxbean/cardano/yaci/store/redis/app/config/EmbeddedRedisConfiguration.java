package com.bloxbean.cardano.yaci.store.redis.app.config;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.RedisBlockStorage;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.RedisBlockStorageReader;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.RedisRollbackStorage;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.mapper.RedisBlockMapper;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.repository.RedisBlockRepository;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.repository.RedisRollbackRepository;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.RedisUtxoStorage;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.RedisUtxoStorageReader;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository.RedisTxInputRepository;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository.RedisUtxoRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import com.redis.om.spring.search.stream.EntityStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRedisDocumentRepositories(basePackages = {
        "com.bloxbean.cardano.yaci.store.extensions.redis.utxo.*",
        "com.bloxbean.cardano.yaci.store.extensions.redis.blocks.*"
})
public class EmbeddedRedisConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UtxoStorage utxoStorage(RedisUtxoRepository utxoRepository, RedisTxInputRepository txInputRepository, UtxoCache utxoCache) {
        return new RedisUtxoStorage(utxoRepository, txInputRepository, utxoCache);
    }

    @Bean
    @ConditionalOnMissingBean
    public UtxoStorageReader utxoStorageReader(RedisUtxoRepository utxoRepository, RedisTxInputRepository txInputRepository) {
        return new RedisUtxoStorageReader(utxoRepository, txInputRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockStorage blockStorage(RedisBlockRepository blockRepository, RedisBlockMapper blockMapper, EntityStream entityStream) {
        return new RedisBlockStorage(blockRepository, blockMapper, entityStream);
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockStorageReader blockStorageReader(RedisBlockRepository blockReadRepository, RedisBlockMapper blockMapper, EntityStream entityStream) {
        return new RedisBlockStorageReader(blockReadRepository, blockMapper, entityStream);
    }

    @Bean
    @ConditionalOnMissingBean
    public RollbackStorage rollbackStorage(RedisRollbackRepository rollbackRepository) {
        return new RedisRollbackStorage(rollbackRepository);
    }
}
