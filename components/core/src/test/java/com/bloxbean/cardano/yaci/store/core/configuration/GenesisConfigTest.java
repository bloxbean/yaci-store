package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class GenesisConfigTest {

    @Test
    void getShelleyGenesisProtocolParams_ReturnsParsedProtocolParams() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(NetworkType.MAINNET.getProtocolMagic());
        GenesisConfig genesisConfig = new GenesisConfig(
                storeProperties, new ObjectMapper(), new DefaultResourceLoader());

        assertThat(genesisConfig.getShelleyGenesisProtocolParams().getKeyDeposit())
                .isEqualTo(BigInteger.valueOf(2_000_000));
    }
}
