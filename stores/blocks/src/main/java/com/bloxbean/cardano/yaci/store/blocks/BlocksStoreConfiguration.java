package com.bloxbean.cardano.yaci.store.blocks;

import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.EpochStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.RollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.BlockStorageImpl;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.EpochStorageImpl;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.RollbackStorageImpl;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.EpochMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.BlockRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.EpochRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.RollbackRepository;
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
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.blocks"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa"})
@EnableRedisDocumentRepositories(basePackages = "com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis")
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.blocks"})
@EnableTransactionManagement
@EnableScheduling
public class BlocksStoreConfiguration {

    private final String springDataRedisHost;

    public BlocksStoreConfiguration(@Value("${spring.data.redis.host}") String springDataRedisHost) {
        this.springDataRedisHost = springDataRedisHost;
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockStorage blockStorage(@Qualifier("blockRepositoryJpa") BlockRepository blockRepository, BlockMapper blockMapper,
                                     @Qualifier("blockRepositoryRedis") com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.BlockRepository blockRepositoryRedis,
                                     com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.mapper.BlockMapper blockMapperRedis) {
        if (StringUtils.isNotEmpty(springDataRedisHost)) {
            return new BlockStorageImpl(blockRepositoryRedis, blockMapperRedis);
        } else {
            return new BlockStorageImpl(blockRepository, blockMapper);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockStorageReader blockStorageReader(BlockRepository blockReadRepository, BlockMapper blockMapper) {
        return new BlockStorageReaderImpl(blockReadRepository, blockMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RollbackStorage rollbackStorage(@Qualifier("rollbackRepositoryJpa") RollbackRepository rollbackRepository) {
        return new RollbackStorageImpl(rollbackRepository);
    }
}
