package com.bloxbean.carano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.domain.GenesisCommitteeMember;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConwayGenesisTest {

    @Test
    void parseConwayGenesis_mainnet_bundled() {
        ConwayGenesis conwayGenesis = new ConwayGenesis(NetworkType.MAINNET.getProtocolMagic());
        ProtocolParams protocolParams = conwayGenesis.getProtocolParams();
        List<GenesisCommitteeMember> committeeMembers = conwayGenesis.getCommitteeMembers();
        Double committeeThreshold = conwayGenesis.getCommitteeThreshold();

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

        assertThat(committeeMembers).isEmpty();
        assertThat(committeeThreshold).isNull();
    }

    @Test
    void parseConwayGenesis_sanchonet_bundled() {
        ConwayGenesis conwayGenesis = new ConwayGenesis(NetworkType.SANCHONET.getProtocolMagic());
        ProtocolParams protocolParams = conwayGenesis.getProtocolParams();
        List<GenesisCommitteeMember> committeeMembers = conwayGenesis.getCommitteeMembers();
        Double committeeThreshold = conwayGenesis.getCommitteeThreshold();

        assertThat(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal().doubleValue()).isEqualTo(0.65);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence().doubleValue()).isEqualTo(0.65);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation().doubleValue()).isEqualTo(0.51);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence().doubleValue()).isEqualTo(0.60);
        assertThat(protocolParams.getPoolVotingThresholds().getPvtPPSecurityGroup().doubleValue()).isEqualTo(0.60);

        assertThat(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence().doubleValue()).isEqualTo(0.65);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution().doubleValue()).isEqualTo(0.75);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation().doubleValue()).isEqualTo(0.60);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup().doubleValue()).isEqualTo(0.67);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup().doubleValue()).isEqualTo(0.75);
        assertThat(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal().doubleValue()).isEqualTo(0.67);

        assertThat(protocolParams.getCommitteeMinSize()).isEqualTo(5);
        assertThat(protocolParams.getCommitteeMaxTermLength()).isEqualTo(146);
        assertThat(protocolParams.getGovActionLifetime()).isEqualTo(14);
        assertThat(protocolParams.getGovActionDeposit()).isEqualTo(BigInteger.valueOf(100000000000L));
        assertThat(protocolParams.getDrepDeposit()).isEqualTo(500000000);
        assertThat(protocolParams.getDrepActivity()).isEqualTo(20);
        assertThat(protocolParams.getMinFeeRefScriptCostPerByte()).isEqualTo(BigDecimal.valueOf(15));

        assertThat(committeeMembers.stream().map(GenesisCommitteeMember::getHash))
                .contains("7ceede7d6a89e006408e6b7c6acb3dd094b3f6817e43b4a36d01535b",
                        "6095e643ea6f1cccb6e463ec34349026b3a48621aac5d512655ab1bf",
                        "27999ed757d6dac217471ae61d69b1b067b8b240d9e3ff36eb66b5d0",
                        "87f867a31c0f81360d4d7dcddb6b025ba8383db9bf77a2af7797799d",
                        "a19a7ba1caede8f3ab3e5e2a928b3798d7d011af18fbd577f7aeb0ec");

        assertThat(committeeMembers.stream().map(GenesisCommitteeMember::getExpiredEpoch))
                .contains(500, 500, 500, 500, 500);
        assertThat(committeeMembers.stream().allMatch(GenesisCommitteeMember::getHasScript)).isTrue();
        assertThat(committeeThreshold).isEqualTo(0.67);
    }
}
