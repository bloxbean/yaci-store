package com.bloxbean.carano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.domain.GenesisCommitteeMember;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;
import static org.assertj.core.api.Assertions.assertThat;

public class ConwayGenesisTest {

    @Test
    void parseConwayGenesis_mainnet_bundled() {
        ConwayGenesis conwayGenesis = new ConwayGenesis(NetworkType.MAINNET.getProtocolMagic());
        ProtocolParams protocolParams = conwayGenesis.getProtocolParams();
        List<GenesisCommitteeMember> committeeMembers = conwayGenesis.getCommitteeMembers();
        BigInteger committeeNumerator = conwayGenesis.getCommitteeNumerator();
        BigInteger committeeDenominator = conwayGenesis.getCommitteeDenominator();
        BigDecimal committeeThreshold = conwayGenesis.getCommitteeThreshold();

        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal()).doubleValue()).isEqualTo(0.51);
        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence()).doubleValue()).isEqualTo(0.51);
        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation()).doubleValue()).isEqualTo(0.51);
        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence()).doubleValue()).isEqualTo(0.51);
        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtPPSecurityGroup()).doubleValue()).isEqualTo(0.51);

        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence()).doubleValue()).isEqualTo(0.6);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution()).doubleValue()).isEqualTo(0.75);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation()).doubleValue()).isEqualTo(0.6);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup()).doubleValue()).isEqualTo(0.75);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal()).doubleValue()).isEqualTo(0.67);

        assertThat(protocolParams.getCommitteeMinSize()).isEqualTo(7);
        assertThat(protocolParams.getCommitteeMaxTermLength()).isEqualTo(146);
        assertThat(protocolParams.getGovActionLifetime()).isEqualTo(6);
        assertThat(protocolParams.getGovActionDeposit()).isEqualTo(100000000000L);
        assertThat(protocolParams.getDrepDeposit()).isEqualTo(500000000);
        assertThat(protocolParams.getDrepActivity()).isEqualTo(20);

        assertThat(committeeMembers.stream().map(GenesisCommitteeMember::getHash))
                .contains("df0e83bde65416dade5b1f97e7f115cc1ff999550ad968850783fe50",
                        "b6012034ba0a7e4afbbf2c7a1432f8824aee5299a48e38e41a952686",
                        "ce8b37a72b178a37bbd3236daa7b2c158c9d3604e7aa667e6c6004b7",
                        "f0dc2c00d92a45521267be2d5de1c485f6f9d14466d7e16062897cf7",
                        "349e55f83e9af24813e6cb368df6a80d38951b2a334dfcdf26815558",
                        "84aebcfd3e00d0f87af918fc4b5e00135f407e379893df7e7d392c6a",
                        "e8165b3328027ee0d74b1f07298cb092fd99aa7697a1436f5997f625");

        assertThat(committeeMembers.stream().map(GenesisCommitteeMember::getExpiredEpoch))
                .contains(580, 580, 580, 580, 580, 580, 580);

        assertThat(committeeNumerator).isEqualTo(2);
        assertThat(committeeDenominator).isEqualTo(3);
        assertThat(committeeThreshold).isEqualTo( new BigDecimal(committeeNumerator).divide(new BigDecimal(committeeDenominator), 10, RoundingMode.HALF_UP));
    }

    @Test
    void parseConwayGenesis_sanchonet_bundled() {
        ConwayGenesis conwayGenesis = new ConwayGenesis(NetworkType.SANCHONET.getProtocolMagic());
        ProtocolParams protocolParams = conwayGenesis.getProtocolParams();
        List<GenesisCommitteeMember> committeeMembers = conwayGenesis.getCommitteeMembers();
        BigInteger committeeNumerator = conwayGenesis.getCommitteeNumerator();
        BigInteger committeeDenominator = conwayGenesis.getCommitteeDenominator();
        BigDecimal committeeThreshold = conwayGenesis.getCommitteeThreshold();

        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal()).doubleValue()).isEqualTo(0.65);
        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence()).doubleValue()).isEqualTo(0.65);
        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation()).doubleValue()).isEqualTo(0.51);
        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence()).doubleValue()).isEqualTo(0.60);
        assertThat(safeRatio(protocolParams.getPoolVotingThresholds().getPvtPPSecurityGroup()).doubleValue()).isEqualTo(0.60);

        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence()).doubleValue()).isEqualTo(0.65);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution()).doubleValue()).isEqualTo(0.75);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation()).doubleValue()).isEqualTo(0.60);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup()).doubleValue()).isEqualTo(0.67);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup()).doubleValue()).isEqualTo(0.75);
        assertThat(safeRatio(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal()).doubleValue()).isEqualTo(0.67);

        assertThat(protocolParams.getCommitteeMinSize()).isEqualTo(5);
        assertThat(protocolParams.getCommitteeMaxTermLength()).isEqualTo(146);
        assertThat(protocolParams.getGovActionLifetime()).isEqualTo(14);
        assertThat(protocolParams.getGovActionDeposit()).isEqualTo(BigInteger.valueOf(100000000000L));
        assertThat(protocolParams.getDrepDeposit()).isEqualTo(500000000);
        assertThat(protocolParams.getDrepActivity()).isEqualTo(20);
        assertThat(safeRatio(protocolParams.getMinFeeRefScriptCostPerByte())).isEqualByComparingTo(BigDecimal.valueOf(15));

        assertThat(committeeMembers.stream().map(GenesisCommitteeMember::getHash))
                .contains("7ceede7d6a89e006408e6b7c6acb3dd094b3f6817e43b4a36d01535b",
                        "6095e643ea6f1cccb6e463ec34349026b3a48621aac5d512655ab1bf",
                        "27999ed757d6dac217471ae61d69b1b067b8b240d9e3ff36eb66b5d0",
                        "87f867a31c0f81360d4d7dcddb6b025ba8383db9bf77a2af7797799d",
                        "a19a7ba1caede8f3ab3e5e2a928b3798d7d011af18fbd577f7aeb0ec");

        assertThat(committeeMembers.stream().map(GenesisCommitteeMember::getExpiredEpoch))
                .contains(500, 500, 500, 500, 500);
        assertThat(committeeMembers.stream().allMatch(GenesisCommitteeMember::getHasScript)).isTrue();
        assertThat(committeeThreshold).isEqualTo(BigDecimal.valueOf(0.67));
        assertThat(committeeNumerator).isNull();
        assertThat(committeeDenominator).isNull();
    }
}
