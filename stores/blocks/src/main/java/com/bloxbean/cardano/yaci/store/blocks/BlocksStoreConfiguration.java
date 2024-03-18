package com.bloxbean.cardano.yaci.store.blocks;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.config.JpaConfig;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.config.RedisConfig;
import com.bloxbean.cardano.yaci.store.blocks.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.JpaBlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.mapper.RedisBlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.JpaBlockRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.JpaRollbackRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.RedisBlockRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.RedisRollbackRepository;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(prefix = "store.blocks", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.blocks")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.*")
@EnableRedisDocumentRepositories(basePackages = "com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.*")
@EntityScan(basePackages = "com.bloxbean.cardano.yaci.store.blocks")
@EnableTransactionManagement
@EnableScheduling
public class BlocksStoreConfiguration {

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${spring.data.redis.host:}')")
    public RedisConfig redisBlockConfig(RedisBlockRepository redisBlockRepository, RedisBlockMapper redisBlockMapper,
                                     RedisRollbackRepository redisRollbackRepository) {
        return new RedisConfig(redisBlockRepository, redisBlockMapper, redisRollbackRepository);
    }

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${spring.datasource.url:}')")
    public JpaConfig JpaBlockConfig(JpaBlockRepository jpaBlockRepository, JpaBlockMapper jpaBlockMapper,
                               JpaRollbackRepository jpaRollbackRepository) {
        return new JpaConfig(jpaBlockRepository, jpaBlockMapper, jpaRollbackRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public StorageConfig blockStorageConfig(RedisConfig redisBlockConfig, JpaConfig JpaBlockConfig) {
        return redisBlockConfig != null ? redisBlockConfig :  JpaBlockConfig;
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockStorage blockStorage(StorageConfig blockStorageConfig) {
        return blockStorageConfig.blockStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockStorageReader blockStorageReader(StorageConfig blockStorageConfig) {
        return blockStorageConfig.blockStorageReader();
    }

    @Bean
    @ConditionalOnMissingBean
    public RollbackStorage rollbackStorage(StorageConfig blockStorageConfig) {
        return blockStorageConfig.rollbackStorage();
    }
}
