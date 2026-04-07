package com.bloxbean.cardano.yaci.store.extensions.assetstore;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReaderImpl;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.TokenLogoRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.TokenMetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReaderImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        name = "store.assets.ext.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(
        basePackages = {"com.bloxbean.cardano.yaci.store.extensions.assetstore"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.bloxbean\\.cardano\\.yaci\\.store\\.extensions\\.assetstore\\.cip113\\..*"
        )
)
@EnableJpaRepositories(
        basePackages = {"com.bloxbean.cardano.yaci.store.extensions.assetstore"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.bloxbean\\.cardano\\.yaci\\.store\\.extensions\\.assetstore\\.cip113\\..*"
        )
)
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.extensions.assetstore"})
@EnableTransactionManagement
@EnableScheduling
public class AssetsExtConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Cip26StorageReader cip26StorageReader(TokenMetadataRepository tokenMetadataRepository,
                                                  TokenLogoRepository tokenLogoRepository) {
        return new Cip26StorageReaderImpl(tokenMetadataRepository, tokenLogoRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public Cip68StorageReader cip68StorageReader(Cip68TokenService cip68TokenService) {
        return new Cip68StorageReaderImpl(cip68TokenService);
    }
}
