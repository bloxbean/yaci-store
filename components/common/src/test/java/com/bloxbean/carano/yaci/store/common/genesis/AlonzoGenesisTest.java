package com.bloxbean.carano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.AlonzoGenesis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class AlonzoGenesisTest {
    public static final String GENESIS_CUSTOM_COSTMODEL_ALONZO_GENESIS_JSON = "/genesis/custom-costmodel-alonzo-genesis.json";

    @Test
    void parseAlonzoGenesis_mainnet_bundled() {
        AlonzoGenesis alonzoGenesis = new AlonzoGenesis(NetworkType.MAINNET.getProtocolMagic());
        ProtocolParams protocolParams = alonzoGenesis.getProtocolParams();

        //protocol parameters
        assertThat(protocolParams.getAdaPerUtxoByte()).isEqualTo(BigInteger.valueOf(34482));
        assertThat(protocolParams.getPriceStep()).isEqualTo(BigDecimal.valueOf(0.0000721));
        assertThat(protocolParams.getPriceMem()).isEqualTo(BigDecimal.valueOf(.0577));
        assertThat(protocolParams.getMaxTxExMem()).isEqualTo(BigInteger.valueOf(10000000));
        assertThat(protocolParams.getMaxTxExSteps()).isEqualTo(BigInteger.valueOf(10000000000L));
        assertThat(protocolParams.getMaxBlockExMem()).isEqualTo(BigInteger.valueOf(50000000L));
        assertThat(protocolParams.getMaxBlockExSteps()).isEqualTo(BigInteger.valueOf(40000000000L));
        assertThat(protocolParams.getMaxValSize()).isEqualTo(5000L);
        assertThat(protocolParams.getCollateralPercent()).isEqualTo(150);
        assertThat(protocolParams.getMaxCollateralInputs()).isEqualTo(3);
        assertThat(protocolParams.getCostModels().get("PlutusV1")).hasSize(166);
    }

    //-- Sanchonet genesis file test
    @Test
    void parseAlonzoGenesis_sanchonet_bundled() {
        AlonzoGenesis alonzoGenesis = new AlonzoGenesis(NetworkType.SANCHONET.getProtocolMagic());
        ProtocolParams protocolParams = alonzoGenesis.getProtocolParams();

        //protocol parameters
        assertThat(protocolParams.getAdaPerUtxoByte()).isEqualTo(BigInteger.valueOf(34482));
        assertThat(protocolParams.getPriceStep()).isEqualTo(BigDecimal.valueOf(0.0000721));
        assertThat(protocolParams.getPriceMem()).isEqualTo(BigDecimal.valueOf(.0577));
        assertThat(protocolParams.getMaxTxExMem()).isEqualTo(BigInteger.valueOf(10000000));
        assertThat(protocolParams.getMaxTxExSteps()).isEqualTo(BigInteger.valueOf(10000000000L));
        assertThat(protocolParams.getMaxBlockExMem()).isEqualTo(BigInteger.valueOf(50000000L));
        assertThat(protocolParams.getMaxBlockExSteps()).isEqualTo(BigInteger.valueOf(40000000000L));
        assertThat(protocolParams.getMaxValSize()).isEqualTo(5000L);
        assertThat(protocolParams.getCollateralPercent()).isEqualTo(150);
        assertThat(protocolParams.getMaxCollateralInputs()).isEqualTo(3);
        assertThat(protocolParams.getCostModels().get("PlutusV1")).hasSize(166);
    }

    @Test
    void parseAlonzoGenesis_sanchonet_customCostModel() {
        AlonzoGenesis alonzoGenesis = new AlonzoGenesis(this.getClass().getResourceAsStream(GENESIS_CUSTOM_COSTMODEL_ALONZO_GENESIS_JSON));
        ProtocolParams protocolParams = alonzoGenesis.getProtocolParams();

        //protocol parameters
        assertThat(protocolParams.getAdaPerUtxoByte()).isEqualTo(BigInteger.valueOf(34482));
        assertThat(protocolParams.getPriceStep()).isEqualTo(BigDecimal.valueOf(0.0000721));
        assertThat(protocolParams.getPriceMem()).isEqualTo(BigDecimal.valueOf(.0577));
        assertThat(protocolParams.getMaxTxExMem()).isEqualTo(BigInteger.valueOf(10000000));
        assertThat(protocolParams.getMaxTxExSteps()).isEqualTo(BigInteger.valueOf(10000000000L));
        assertThat(protocolParams.getMaxBlockExMem()).isEqualTo(BigInteger.valueOf(50000000L));
        assertThat(protocolParams.getMaxBlockExSteps()).isEqualTo(BigInteger.valueOf(40000000000L));
        assertThat(protocolParams.getMaxValSize()).isEqualTo(5000L);
        assertThat(protocolParams.getCollateralPercent()).isEqualTo(150);
        assertThat(protocolParams.getMaxCollateralInputs()).isEqualTo(3);
        assertThat(protocolParams.getCostModels().get("PlutusV1")).hasSize(166);
    }

}
