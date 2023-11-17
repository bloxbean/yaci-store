package com.bloxbean.cardano.yaci.store.api.blocks;

import com.bloxbean.cardano.yaci.store.api.blocks.storage.BlockReader;
import com.bloxbean.cardano.yaci.store.api.blocks.storage.EpochReader;
import com.bloxbean.cardano.yaci.store.api.blocks.storage.impl.BlockReaderImpl;
import com.bloxbean.cardano.yaci.store.api.blocks.storage.impl.EpochReaderImpl;
import com.bloxbean.cardano.yaci.store.api.blocks.storage.impl.repository.BlockReadRepository;
import com.bloxbean.cardano.yaci.store.api.blocks.storage.impl.repository.EpochReadRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.EpochMapper;
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
        name = "api-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.blocks"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.api.blocks.storage"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.blocks.storage"})
@EnableTransactionManagement
@EnableScheduling
public class BlocksApiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BlockReader blockReader(BlockReadRepository blockReadRepository, BlockMapper blockMapper) {
        return new BlockReaderImpl(blockReadRepository, blockMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EpochReader epochReader(EpochReadRepository epochReadRepository, EpochMapper epochMapper) {
        return new EpochReaderImpl(epochReadRepository, epochMapper);
    }

}
