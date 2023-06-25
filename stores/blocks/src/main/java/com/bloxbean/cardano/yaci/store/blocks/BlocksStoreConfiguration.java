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
    public EpochStorage epochStorage(EpochRepository epochRepository, EpochMapper epochMapper) {
        return new EpochStorageImpl(epochRepository, epochMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RollbackStorage rollbackStorage(RollbackRepository rollbackRepository) {
        return new RollbackStorageImpl(rollbackRepository);
    }
}
