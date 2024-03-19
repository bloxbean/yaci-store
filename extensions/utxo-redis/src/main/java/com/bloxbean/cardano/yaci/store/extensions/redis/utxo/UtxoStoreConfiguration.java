package com.bloxbean.cardano.yaci.store.extensions.redis.utxo;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "store.utxo", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableRedisDocumentRepositories(basePackages = "com.bloxbean.cardano.yaci.store.extensions.redis.utxo.*")
public class UtxoStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UtxoStorage utxoStorage(RedisUtxoRepository utxoRepository, RedisTxInputRepository txInputRepository, UtxoCache utxoCache) {
        return new RedisUtxoStorage(utxoRepository, txInputRepository, utxoCache);
    }

    @Bean
    @ConditionalOnMissingBean
    public UtxoStorageReader utxoStorageReader(RedisUtxoRepository utxoRepository, RedisTxInputRepository txInputRepository, EntityStream entityStream) {
        return new RedisUtxoStorageReader(utxoRepository, txInputRepository, entityStream);
    }
}
