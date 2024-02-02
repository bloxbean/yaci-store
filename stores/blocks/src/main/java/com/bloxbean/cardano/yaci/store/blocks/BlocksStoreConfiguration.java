package com.bloxbean.cardano.yaci.store.blocks;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.BlockStorageImpl;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.BlockStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.RollbackStorageImpl;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.RollbackRepository;
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
@ConditionalOnProperty(
        prefix = "store.blocks",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.blocks"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.blocks"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.blocks"})
@EnableTransactionManagement
@EnableScheduling
public class BlocksStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BlockStorage blockStorage(BlockRepository blockRepository, BlockMapper blockMapper) {
        return new BlockStorageImpl(blockRepository, blockMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockStorageReader blockStorageReader(BlockRepository blockReadRepository, BlockMapper blockMapper) {
        return new BlockStorageReaderImpl(blockReadRepository, blockMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RollbackStorage rollbackStorage(RollbackRepository rollbackRepository) {
        return new RollbackStorageImpl(rollbackRepository);
    }
}
