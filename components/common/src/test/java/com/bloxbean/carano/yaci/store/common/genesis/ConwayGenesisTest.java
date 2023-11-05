package com.bloxbean.carano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConwayGenesisTest {

    @Test
    void parseConwayGenesis_mainnet_bundled() {
        ConwayGenesis conwayGenesis = new ConwayGenesis(NetworkType.MAINNET.getProtocolMagic());
        ProtocolParams protocolParams = conwayGenesis.getProtocolParams();

        assertThat(protocolParams).isNull();
    }
}
