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

        assertThat(protocolParams.getCommitteeMinSize()).isEqualTo(3);
        assertThat(protocolParams.getCommitteeMaxTermLength()).isEqualTo(73);
        assertThat(protocolParams.getGovActionLifetime()).isEqualTo(8);
        assertThat(protocolParams.getGovActionDeposit()).isEqualTo(BigInteger.valueOf(50000000000L));
        assertThat(protocolParams.getDrepDeposit()).isEqualTo(500000000);
        assertThat(protocolParams.getDrepActivity()).isEqualTo(20);
        assertThat(protocolParams.getMinFeeRefScriptCostPerByte()).isEqualTo(BigDecimal.valueOf(44));

        assertThat(committeeMembers.stream().map(GenesisCommitteeMember::getHash))
                .contains("8fc13431159fdda66347a38c55105d50d77d67abc1c368b876d52ad1",
                        "921e1ccb4812c4280510c9ccab81c561f3d413e7d744d48d61215d1f",
                        "d5d09d9380cf9dcde1f3c6cd88b08ca9e00a3d550022ca7ee4026342",
                        "2c698e41831684b16477fb50082b0c0e396d436504e39037d5366582");

        assertThat(committeeMembers.stream().map(GenesisCommitteeMember::getExpiredEpoch))
                .contains(336, 400, 400, 400);
        assertThat(committeeMembers.stream().allMatch(GenesisCommitteeMember::getHasScript)).isTrue();
        assertThat(committeeThreshold).isEqualTo(0.66);
    }
}
