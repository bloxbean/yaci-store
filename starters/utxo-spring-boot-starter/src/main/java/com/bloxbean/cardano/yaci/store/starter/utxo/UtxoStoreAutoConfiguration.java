package com.bloxbean.cardano.yaci.store.starter.utxo;

import com.bloxbean.cardano.yaci.store.api.utxo.UtxoApiConfiguration;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(UtxoStoreProperties.class)
@Import({UtxoStoreConfiguration.class, UtxoApiConfiguration.class})
@Slf4j
public class UtxoStoreAutoConfiguration {

    @Autowired
    UtxoStoreProperties properties;
}
