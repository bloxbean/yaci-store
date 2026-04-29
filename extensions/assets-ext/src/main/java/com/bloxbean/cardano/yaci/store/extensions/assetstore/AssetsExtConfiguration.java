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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Clock;

@Configuration
@ConditionalOnProperty(
        name = "store.assets.ext.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.extensions.assetstore"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.extensions.assetstore"})
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

    /**
     * TEMPORARY: ships a {@link Clock} bean here so the assets-ext services
     * (TokenMetadataService, Cip68Processor, etc.) can be injected with one.
     *
     * <p>TODO: Remove this method once
     * https://github.com/bloxbean/yaci-store/pull/912 lands on main and this
     * branch rebases. That PR adds {@code @Bean Clock systemClock()} to
     * {@code components/core/StoreConfiguration} as the canonical home for
     * this bean — every yaci-store deployment then has it, not just ones
     * that include assets-ext. {@link ConditionalOnMissingBean} on this
     * method ensures core's bean wins after rebase, so removal is a no-op
     * for runtime behaviour.
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
