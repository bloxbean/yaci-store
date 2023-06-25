package com.bloxbean.cardano.yaci.store.assets;

import com.bloxbean.cardano.yaci.store.assets.storage.AssetMapper;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorageImpl;
import com.bloxbean.cardano.yaci.store.assets.storage.repository.TxAssetRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.assets",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.assets"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.assets"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.assets"})
@EnableTransactionManagement
public class AssetsStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AssetStorage assetStorage(TxAssetRepository txAssetRepository, AssetMapper assetMapper) {
        return new AssetStorageImpl(txAssetRepository, assetMapper);
    }
}
