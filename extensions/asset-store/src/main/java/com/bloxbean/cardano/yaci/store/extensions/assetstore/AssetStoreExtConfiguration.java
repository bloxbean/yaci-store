package com.bloxbean.cardano.yaci.store.extensions.assetstore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        name = "store.extensions.asset-store.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.extensions.assetstore"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.extensions.assetstore"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.extensions.assetstore"})
@EnableTransactionManagement
@EnableScheduling
public class AssetStoreExtConfiguration {
    public static final String STORE_ASSET_STORE_EXT_ENABLED = "store.extensions.asset-store.enabled";
}
