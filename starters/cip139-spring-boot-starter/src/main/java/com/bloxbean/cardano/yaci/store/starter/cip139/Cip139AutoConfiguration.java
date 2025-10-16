package com.bloxbean.cardano.yaci.store.starter.cip139;

import com.bloxbean.cardano.yaci.store.cip139.protocolparameters.CIP139ProtocolParametersConfiguration;
import com.bloxbean.cardano.yaci.store.cip139.transaction.CIP139TransactionConfiguration;
import com.bloxbean.cardano.yaci.store.cip139.utxo.CIP139UtxoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(Cip139Properties.class)
@Import({CIP139ProtocolParametersConfiguration.class, CIP139TransactionConfiguration.class, CIP139UtxoConfiguration.class})
@Slf4j
public class Cip139AutoConfiguration {

    @Autowired
    Cip139Properties properties;

}
