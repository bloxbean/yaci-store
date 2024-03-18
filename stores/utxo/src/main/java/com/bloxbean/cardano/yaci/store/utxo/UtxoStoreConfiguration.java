package com.bloxbean.cardano.yaci.store.utxo;

import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.config.JpaConfig;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.config.RedisConfig;
import com.bloxbean.cardano.yaci.store.utxo.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaUtxoRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.repository.RedisTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.repository.RedisUtxoRepository;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import com.redis.om.spring.search.stream.EntityStream;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(prefix = "store.utxo", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.utxo")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.*")
@EnableRedisDocumentRepositories(basePackages = "com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.*")
@EntityScan(basePackages = "com.bloxbean.cardano.yaci.store.utxo")
@EnableTransactionManagement
public class UtxoStoreConfiguration {

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${spring.data.redis.host:}')")
    public StorageConfig redisUtxoConfig(RedisUtxoRepository redisUtxoRepository, RedisTxInputRepository redisTxInputRepository, EntityStream entityStream, UtxoCache utxoCache) {
        return new RedisConfig(redisUtxoRepository, redisTxInputRepository, entityStream, utxoCache);
    }

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${spring.datasource.url:}')")
    public StorageConfig JpaUtxoConfig(JpaUtxoRepository jpaUtxoRepository, JpaTxInputRepository spentOutputRepository,
                                       DSLContext dslContext, UtxoCache utxoCache) {
        return new JpaConfig(jpaUtxoRepository, spentOutputRepository, dslContext, utxoCache);
    }

    @Bean
    @ConditionalOnMissingBean
    public UtxoStorage utxoStorage(StorageConfig storageConfig) {
        return storageConfig.utxoStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public UtxoStorageReader utxoStorageReader(StorageConfig storageConfig) {
        return storageConfig.utxoStorageReader();
    }
}
