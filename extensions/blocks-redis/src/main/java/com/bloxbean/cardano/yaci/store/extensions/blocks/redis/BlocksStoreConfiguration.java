package com.bloxbean.cardano.yaci.store.extensions.blocks.redis;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.RedisBlockStorage;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.RedisBlockStorageReader;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.RedisRollbackStorage;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.mapper.RedisBlockMapper;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.repository.RedisBlockRepository;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.repository.RedisRollbackRepository;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "store.blocks", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableRedisDocumentRepositories(basePackages = "com.bloxbean.cardano.yaci.store.extensions.blocks.redis.*")
public class BlocksStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BlockStorage blockStorage(RedisBlockRepository blockRepository, RedisBlockMapper blockMapper) {
        return new RedisBlockStorage(blockRepository, blockMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockStorageReader blockStorageReader(RedisBlockRepository blockReadRepository, RedisBlockMapper blockMapper) {
        return new RedisBlockStorageReader(blockReadRepository, blockMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RollbackStorage rollbackStorage(RedisRollbackRepository rollbackRepository) {
        return new RedisRollbackStorage(rollbackRepository);
    }
}
