package com.bloxbean.cardano.yaci.store.starter.utxo;

import com.bloxbean.cardano.yaci.store.api.utxo.UtxoApiConfiguration;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreConfiguration;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(UtxoStoreAutoConfigProperties.class)
@Import({UtxoStoreConfiguration.class, UtxoApiConfiguration.class})
@Slf4j
public class UtxoStoreAutoConfiguration {

    @Autowired
    UtxoStoreAutoConfigProperties properties;

    @Bean
    public UtxoStoreProperties utxoStoreProperties() {
        var utxoStoreProperties = new UtxoStoreProperties();

        utxoStoreProperties.setSaveAddress(properties.getUtxo().isSaveAddress());
        utxoStoreProperties.setAddressCacheEnabled(properties.getUtxo().isAddressCacheEnabled());
        utxoStoreProperties.setAddressCacheSize(properties.getUtxo().getAddressCacheSize());
        utxoStoreProperties.setAddressCacheExpiryAfterAccess(properties.getUtxo().getAddressCacheExpiryAfterAccess());

        utxoStoreProperties.setPruningEnabled(properties.getUtxo().isPruningEnabled());
        utxoStoreProperties.setPruningInterval(properties.getUtxo().getPruningInterval());
        utxoStoreProperties.setPruningSafeBlocks(properties.getUtxo().getPruningSafeBlocks());
        utxoStoreProperties.setContentAwareRollback(properties.getUtxo().isContentAwareRollback());

        return utxoStoreProperties;
    }
}
