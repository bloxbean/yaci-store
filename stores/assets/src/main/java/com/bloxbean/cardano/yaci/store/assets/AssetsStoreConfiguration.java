package com.bloxbean.cardano.yaci.store.assets;

import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorageReader;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.AssetStorageImpl;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.AssetStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.mapper.AssetMapper;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.repository.TxAssetRepository;
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
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.assets.storage"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.assets.storage"})
@EnableTransactionManagement
public class AssetsStoreConfiguration {
    public final static String STORE_ASSETS_ENABLED = "store.assets.enabled";

    @Bean
    @ConditionalOnMissingBean
    public AssetStorage assetStorage(TxAssetRepository txAssetRepository, AssetMapper assetMapper) {
        return new AssetStorageImpl(txAssetRepository, assetMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AssetStorageReader assetStorageReader(TxAssetRepository txAssetReadRepository, AssetMapper assetMapper) {
        return new AssetStorageReaderImpl(txAssetReadRepository, assetMapper);
    }
}
