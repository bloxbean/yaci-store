package com.bloxbean.cardano.yaci.store.api.assets;

import com.bloxbean.cardano.yaci.store.api.assets.storage.AssetReader;
import com.bloxbean.cardano.yaci.store.api.assets.storage.AssetReaderImpl;
import com.bloxbean.cardano.yaci.store.api.assets.storage.repository.TxAssetReadRepository;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetMapper;
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
        name = "api-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.assets"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.api.assets.storage"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.assets.storage"})
@EnableTransactionManagement
public class AssetsApiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AssetReader assetReader(TxAssetReadRepository txAssetReadRepository, AssetMapper assetMapper) {
        return new AssetReaderImpl(txAssetReadRepository, assetMapper);
    }
}
