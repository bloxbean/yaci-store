package com.bloxbean.carano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ConwayGenesisTest {

    @Test
    void parseConwayGenesis_mainnet_bundled() {
        ConwayGenesis conwayGenesis = new ConwayGenesis(NetworkType.MAINNET.getProtocolMagic());
        ProtocolParams protocolParams = conwayGenesis.getProtocolParams();

        assertThat(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtPPSecurityGroup().doubleValue()).isEqualTo(0.51);

        assertThat(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal().doubleValue()).isEqualTo(0.51);

        assertThat(protocolParams.getCommitteeMinSize()).isEqualTo(0);
        assertThat(protocolParams.getCommitteeMaxTermLength()).isEqualTo(200);
        assertThat(protocolParams.getGovActionLifetime()).isEqualTo(10);
        assertThat(protocolParams.getGovActionDeposit()).isEqualTo(1000000000);
        assertThat(protocolParams.getDrepDeposit()).isEqualTo(2000000);
        assertThat(protocolParams.getDrepActivity()).isEqualTo(20);
    }

    @Test
    void parseConwayGenesis_sanchonet_bundled() {
        ConwayGenesis conwayGenesis = new ConwayGenesis(NetworkType.SANCHONET.getProtocolMagic());
        ProtocolParams protocolParams = conwayGenesis.getProtocolParams();

        assertThat(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal().doubleValue()).isEqualTo(0.60);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence().doubleValue()).isEqualTo(0.60);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtPPSecurityGroup().doubleValue()).isEqualTo(0.60);

        assertThat(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence().doubleValue()).isEqualTo(0.60);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution().doubleValue()).isEqualTo(0.75);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation().doubleValue()).isEqualTo(0.60);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup().doubleValue()).isEqualTo(0.75);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal().doubleValue()).isEqualTo(0.67);

        assertThat(protocolParams.getCommitteeMinSize()).isEqualTo(7);
        assertThat(protocolParams.getCommitteeMaxTermLength()).isEqualTo(73);
        assertThat(protocolParams.getGovActionLifetime()).isEqualTo(6);
        assertThat(protocolParams.getGovActionDeposit()).isEqualTo(BigInteger.valueOf(50000000000L));
        assertThat(protocolParams.getDrepDeposit()).isEqualTo(500000000);
        assertThat(protocolParams.getDrepActivity()).isEqualTo(20);
        assertThat(protocolParams.getMinFeeRefScriptCostPerByte()).isEqualTo(BigDecimal.valueOf(44));
    }
}
