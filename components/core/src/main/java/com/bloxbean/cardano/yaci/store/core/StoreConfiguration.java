package com.bloxbean.cardano.yaci.store.core;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.service.CursorCleanupScheduler;
import com.bloxbean.cardano.yaci.store.core.storage.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.config.JpaConfig;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.config.RedisConfig;
import com.bloxbean.cardano.yaci.store.core.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.mapper.JpaEraMapper;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.mapper.RedisEraMapper;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository.JpaCursorRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository.JpaEraRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.repository.RedisCursorRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.repository.RedisEraRepository;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Configuration
@EnableScheduling
@EnableTransactionManagement
@EntityScan(basePackages = "com.bloxbean.cardano.yaci.store.core")
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.core")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.*")
@ConditionalOnProperty(prefix = "store.core", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableRedisDocumentRepositories(basePackages = "com.bloxbean.cardano.yaci.store.core.storage.impl.redis.*")
public class StoreConfiguration {

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${spring.data.redis.host:}')")
    public RedisConfig redisCursorConfig(RedisCursorRepository redisCursorRepository, RedisEraRepository redisEraRepository,
                                        RedisEraMapper redisEraMapper) {
        return new RedisConfig(redisCursorRepository, redisEraRepository, redisEraMapper);
    }

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${spring.datasource.url:}')")
    public JpaConfig JpaCursorConfig(JpaCursorRepository jpaCursorRepository, JpaEraRepository jpaEraRepository,
                                    JpaEraMapper jpaEraMapper) {
        return new JpaConfig(jpaCursorRepository, jpaEraRepository, jpaEraMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public StorageConfig blockStorageConfig(RedisConfig redisCursorConfig, JpaConfig JpaCursorConfig) {
        return redisCursorConfig != null ? redisCursorConfig :  JpaCursorConfig;
    }

    @Bean
    @ConditionalOnMissingBean
    public CursorStorage jpaCursorStorage(StorageConfig storageConfig) {
        return storageConfig.cursorStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public EraStorage eraStorage(StorageConfig storageConfig) {
        return storageConfig.eraStorage();
    }

    @Bean
    @ConditionalOnExpression("${store.cardano.cursor-no-of-blocks-to-keep:1} > 0")
    public CursorCleanupScheduler cursorCleanupScheduler(CursorStorage cursorStorage, StoreProperties storeProperties) {
        log.info("<<< Enable CursorCleanupScheduler >>>");
        log.info("CursorCleanupScheduler will run every {} sec", storeProperties.getCursorCleanupInterval());
        log.info("CursorCleanupScheduler will keep {} blocks in cursor", storeProperties.getCursorNoOfBlocksToKeep());
        return new CursorCleanupScheduler(cursorStorage, storeProperties);
    }
}
