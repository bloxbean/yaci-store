package com.bloxbean.cardano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ShelleyGenesisProtocolParamsProviderTest {

    @Test
    void getProtocolParams_returnsConfiguredNetworkGenesisParams() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(NetworkType.MAINNET.getProtocolMagic());

        ShelleyGenesisProtocolParamsProvider provider = new ShelleyGenesisProtocolParamsProvider(storeProperties);

        assertThat(provider.getProtocolParams())
                .map(ProtocolParams::getKeyDeposit)
                .contains(BigInteger.valueOf(2_000_000));
    }

    @Test
    void getProtocolParams_returnsEmptyWhenGenesisIsUnavailable() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(42);

        ShelleyGenesisProtocolParamsProvider provider = new ShelleyGenesisProtocolParamsProvider(storeProperties);

        assertThat(provider.getProtocolParams()).isEmpty();
    }
}
