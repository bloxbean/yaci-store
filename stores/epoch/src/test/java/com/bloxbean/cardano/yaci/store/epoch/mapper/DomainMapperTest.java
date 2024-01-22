package com.bloxbean.cardano.yaci.store.epoch.mapper;

import com.bloxbean.cardano.yaci.core.model.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.core.model.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DomainMapperTest {

    DomainMapperImpl domainMapper = new DomainMapperImpl();

    @Test
    void toProtocolParams() {
        ProtocolParamUpdate protocolParamUpdate = ProtocolParamUpdate.builder()
                .minFeeA(100)
                .minFeeB(200)
                .maxBlockSize(1000)
                .maxTxSize(2000)
                .maxBlockHeaderSize(3000)
                .keyDeposit(BigInteger.valueOf(4000))
                .poolDeposit(BigInteger.valueOf(5000))
                .maxEpoch(100)
                .nOpt(200)
                .minUtxo(BigInteger.valueOf(1000))
                .poolPledgeInfluence(BigDecimal.valueOf(0.30))
                .expansionRate(new BigDecimal("0.40"))
                .treasuryGrowthRate(new BigDecimal("0.50"))
                .decentralisationParam(new BigDecimal("0.60"))
                .extraEntropy(new Tuple<>(1, "0x5678"))
                .protocolMajorVer(2)
                .protocolMinorVer(3)
                .minPoolCost(BigInteger.valueOf(6000))
                .adaPerUtxoByte(BigInteger.valueOf(7000))
                .maxTxExMem(BigInteger.valueOf(8000))
                .maxTxExSteps(BigInteger.valueOf(9000))
                .maxBlockExMem(BigInteger.valueOf(10000))
                .maxBlockExSteps(BigInteger.valueOf(11000))
                .maxValSize(12000L)
                .collateralPercent(13000)
                .maxValSize(8000L)
                .collateralPercent(9000)
                .maxCollateralInputs(10000)
                .poolVotingThresholds(PoolVotingThresholds.builder()
                        .pvtCommitteeNormal(new BigDecimal("0.10"))
                        .pvtCommitteeNoConfidence(new BigDecimal("0.20"))
                        .pvtHardForkInitiation(new BigDecimal("0.30"))
                        .pvtMotionNoConfidence(new BigDecimal("0.40"))
                    .build()
                )
                .drepVotingThresholds(DrepVoteThresholds.builder()
                        .dvtMotionNoConfidence(new BigDecimal("0.10"))
                        .dvtCommitteeNormal(new BigDecimal("0.20"))
                        .dvtCommitteeNoConfidence(new BigDecimal("0.30"))
                        .dvtUpdateToConstitution(new BigDecimal("0.40"))
                        .dvtHardForkInitiation(new BigDecimal("0.50"))
                        .dvtPPNetworkGroup(new BigDecimal("0.60"))
                        .dvtPPEconomicGroup(new BigDecimal("0.70"))
                        .dvtPPTechnicalGroup(new BigDecimal("0.80"))
                        .dvtPPGovGroup(new BigDecimal("0.90"))
                        .dvtTreasuryWithdrawal(new BigDecimal("0.100"))
                        .build()
                )
                .committeeMinSize(1000)
                .committeeMaxTermLength(10)
                .govActionLifetime(5000)
                .govActionDeposit(BigInteger.valueOf(6000))
                .drepDeposit(BigInteger.valueOf(10000))
                .drepActivity(7000)
                .build();

        ProtocolParams protocolParams = domainMapper.toProtocolParams(protocolParamUpdate);

        assertThat(protocolParams.getMinFeeA()).isEqualTo(protocolParamUpdate.getMinFeeA());
        assertThat(protocolParams.getMinFeeB()).isEqualTo(protocolParamUpdate.getMinFeeB());
        assertThat(protocolParams.getMaxBlockSize()).isEqualTo(protocolParamUpdate.getMaxBlockSize());
        assertThat(protocolParams.getMaxTxSize()).isEqualTo(protocolParamUpdate.getMaxTxSize());
        assertThat(protocolParams.getMaxBlockHeaderSize()).isEqualTo(protocolParamUpdate.getMaxBlockHeaderSize());
        assertThat(protocolParams.getKeyDeposit()).isEqualTo(protocolParamUpdate.getKeyDeposit());
        assertThat(protocolParams.getPoolDeposit()).isEqualTo(protocolParamUpdate.getPoolDeposit());
        assertThat(protocolParams.getMaxEpoch()).isEqualTo(protocolParamUpdate.getMaxEpoch());
        assertThat(protocolParams.getNOpt()).isEqualTo(protocolParamUpdate.getNOpt());
        assertThat(protocolParams.getMinUtxo()).isEqualTo(protocolParamUpdate.getMinUtxo());
        assertThat(protocolParams.getPoolPledgeInfluence()).isEqualTo(protocolParamUpdate.getPoolPledgeInfluence());
        assertThat(protocolParams.getExpansionRate()).isEqualTo(protocolParamUpdate.getExpansionRate());
        assertThat(protocolParams.getTreasuryGrowthRate()).isEqualTo(protocolParamUpdate.getTreasuryGrowthRate());
        assertThat(protocolParams.getDecentralisationParam()).isEqualTo(protocolParamUpdate.getDecentralisationParam());
        assertThat(protocolParams.getProtocolMajorVer()).isEqualTo(protocolParamUpdate.getProtocolMajorVer());
        assertThat(protocolParams.getProtocolMinorVer()).isEqualTo(protocolParamUpdate.getProtocolMinorVer());
        assertThat(protocolParams.getMinPoolCost()).isEqualTo(protocolParamUpdate.getMinPoolCost());
        assertThat(protocolParams.getAdaPerUtxoByte()).isEqualTo(protocolParamUpdate.getAdaPerUtxoByte());
        assertThat(protocolParams.getMaxTxExMem()).isEqualTo(protocolParamUpdate.getMaxTxExMem());
        assertThat(protocolParams.getMaxTxExSteps()).isEqualTo(protocolParamUpdate.getMaxTxExSteps());
        assertThat(protocolParams.getMaxBlockExMem()).isEqualTo(protocolParamUpdate.getMaxBlockExMem());
        assertThat(protocolParams.getMaxBlockExSteps()).isEqualTo(protocolParamUpdate.getMaxBlockExSteps());
        assertThat(protocolParams.getMaxValSize()).isEqualTo(protocolParamUpdate.getMaxValSize());
        assertThat(protocolParams.getCollateralPercent()).isEqualTo(protocolParamUpdate.getCollateralPercent());
        assertThat(protocolParams.getMaxCollateralInputs()).isEqualTo(protocolParamUpdate.getMaxCollateralInputs());

        assertThat(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal()).isEqualTo(protocolParamUpdate.getPoolVotingThresholds().getPvtCommitteeNormal());
        assertThat(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence()).isEqualTo(protocolParamUpdate.getPoolVotingThresholds().getPvtCommitteeNoConfidence());
        assertThat(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation()).isEqualTo(protocolParamUpdate.getPoolVotingThresholds().getPvtHardForkInitiation());
        assertThat(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence()).isEqualTo(protocolParamUpdate.getPoolVotingThresholds().getPvtMotionNoConfidence());

        assertThat(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtMotionNoConfidence());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtCommitteeNormal());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtCommitteeNoConfidence());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtUpdateToConstitution());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtHardForkInitiation());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtPPNetworkGroup());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtPPEconomicGroup());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtPPTechnicalGroup());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtPPGovGroup());
        assertThat(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal()).isEqualTo(protocolParamUpdate.getDrepVotingThresholds().getDvtTreasuryWithdrawal());

        assertThat(protocolParams.getCommitteeMinSize()).isEqualTo(protocolParamUpdate.getCommitteeMinSize());
        assertThat(protocolParams.getCommitteeMaxTermLength()).isEqualTo(protocolParamUpdate.getCommitteeMaxTermLength());
        assertThat(protocolParams.getGovActionLifetime()).isEqualTo(protocolParamUpdate.getGovActionLifetime());
        assertThat(protocolParams.getGovActionDeposit()).isEqualTo(protocolParamUpdate.getGovActionDeposit());
        assertThat(protocolParams.getDrepDeposit()).isEqualTo(protocolParamUpdate.getDrepDeposit());
        assertThat(protocolParams.getDrepActivity()).isEqualTo(protocolParamUpdate.getDrepActivity());
    }

    @Test
    void toProtocolParamsDto() {
        ProtocolParams protocolParams = ProtocolParams.builder()
                .minFeeA(100)
                .minFeeB(200)
                .maxBlockSize(1000)
                .maxTxSize(2000)
                .maxBlockHeaderSize(3000)
                .keyDeposit(BigInteger.valueOf(4000))
                .poolDeposit(BigInteger.valueOf(5000))
                .maxEpoch(100)
                .nOpt(200)
                .minUtxo(BigInteger.valueOf(1000))
                .poolPledgeInfluence(BigDecimal.valueOf(0.30))
                .expansionRate(new BigDecimal("0.40"))
                .treasuryGrowthRate(new BigDecimal("0.50"))
                .decentralisationParam(new BigDecimal("0.60"))
                .extraEntropy("extra entropy str")
                .protocolMajorVer(2)
                .protocolMinorVer(3)
                .minPoolCost(BigInteger.valueOf(6000))
                .adaPerUtxoByte(BigInteger.valueOf(7000))
                .maxTxExMem(BigInteger.valueOf(8000))
                .maxTxExSteps(BigInteger.valueOf(9000))
                .maxBlockExMem(BigInteger.valueOf(10000))
                .maxBlockExSteps(BigInteger.valueOf(11000))
                .maxValSize(12000L)
                .collateralPercent(13000)
                .maxValSize(8000L)
                .collateralPercent(9000)
                .maxCollateralInputs(10000)
                .poolVotingThresholds(com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds.builder()
                        .pvtCommitteeNormal(new BigDecimal("0.10"))
                        .pvtCommitteeNoConfidence(new BigDecimal("0.20"))
                        .pvtHardForkInitiation(new BigDecimal("0.30"))
                        .pvtMotionNoConfidence(new BigDecimal("0.40"))
                        .build()
                )
                .drepVotingThresholds(com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds.builder()
                        .dvtMotionNoConfidence(new BigDecimal("0.10"))
                        .dvtCommitteeNormal(new BigDecimal("0.20"))
                        .dvtCommitteeNoConfidence(new BigDecimal("0.30"))
                        .dvtUpdateToConstitution(new BigDecimal("0.40"))
                        .dvtHardForkInitiation(new BigDecimal("0.50"))
                        .dvtPPNetworkGroup(new BigDecimal("0.60"))
                        .dvtPPEconomicGroup(new BigDecimal("0.70"))
                        .dvtPPTechnicalGroup(new BigDecimal("0.80"))
                        .dvtPPGovGroup(new BigDecimal("0.90"))
                        .dvtTreasuryWithdrawal(new BigDecimal("0.100"))
                        .build()
                )
                .committeeMinSize(1000)
                .committeeMaxTermLength(10)
                .govActionLifetime(5000)
                .govActionDeposit(BigInteger.valueOf(6000))
                .drepDeposit(BigInteger.valueOf(10000))
                .drepActivity(7000)
                .build();

        ProtocolParamsDto cclProtocolParams = domainMapper.toProtocolParamsDto(protocolParams);

        assertThat(cclProtocolParams.getMinFeeA()).isEqualTo(protocolParams.getMinFeeA());
        assertThat(cclProtocolParams.getMinFeeB()).isEqualTo(protocolParams.getMinFeeB());
        assertThat(cclProtocolParams.getMaxBlockSize()).isEqualTo(protocolParams.getMaxBlockSize());
        assertThat(cclProtocolParams.getMaxTxSize()).isEqualTo(protocolParams.getMaxTxSize());
        assertThat(cclProtocolParams.getMaxBlockHeaderSize()).isEqualTo(protocolParams.getMaxBlockHeaderSize());
        assertThat(cclProtocolParams.getKeyDeposit()).isEqualTo(protocolParams.getKeyDeposit().toString());
        assertThat(cclProtocolParams.getPoolDeposit()).isEqualTo(protocolParams.getPoolDeposit().toString());
        assertThat(cclProtocolParams.getEMax()).isEqualTo(protocolParams.getMaxEpoch());
        assertThat(cclProtocolParams.getNOpt()).isEqualTo(protocolParams.getNOpt());
        assertThat(cclProtocolParams.getMinUtxo()).isEqualTo(protocolParams.getMinUtxo().toString());
        assertThat(cclProtocolParams.getA0()).isEqualTo(protocolParams.getPoolPledgeInfluence());
        assertThat(cclProtocolParams.getRho()).isEqualTo(protocolParams.getExpansionRate());
        assertThat(cclProtocolParams.getTau()).isEqualTo(protocolParams.getTreasuryGrowthRate());
        assertThat(cclProtocolParams.getDecentralisationParam()).isEqualTo(protocolParams.getDecentralisationParam());
        assertThat(cclProtocolParams.getExtraEntropy()).isEqualTo(protocolParams.getExtraEntropy());
        assertThat(cclProtocolParams.getProtocolMajorVer()).isEqualTo(protocolParams.getProtocolMajorVer());
        assertThat(cclProtocolParams.getProtocolMinorVer()).isEqualTo(protocolParams.getProtocolMinorVer());
        assertThat(cclProtocolParams.getMinPoolCost()).isEqualTo(protocolParams.getMinPoolCost().toString());
        assertThat(cclProtocolParams.getCoinsPerUtxoSize()).isEqualTo(protocolParams.getAdaPerUtxoByte().toString());
        assertThat(cclProtocolParams.getMaxTxExMem()).isEqualTo(protocolParams.getMaxTxExMem().toString());
        assertThat(cclProtocolParams.getMaxTxExSteps()).isEqualTo(protocolParams.getMaxTxExSteps().toString());
        assertThat(cclProtocolParams.getMaxBlockExMem()).isEqualTo(protocolParams.getMaxBlockExMem().toString());
        assertThat(cclProtocolParams.getMaxBlockExSteps()).isEqualTo(protocolParams.getMaxBlockExSteps().toString());
        assertThat(cclProtocolParams.getMaxValSize()).isEqualTo(protocolParams.getMaxValSize().toString());
        assertThat(cclProtocolParams.getCollateralPercent()).isEqualTo(protocolParams.getCollateralPercent().toString());
        assertThat(cclProtocolParams.getMaxCollateralInputs()).isEqualTo(protocolParams.getMaxCollateralInputs());

        assertThat(cclProtocolParams.getPvtCommitteeNormal()).isEqualTo(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal());
        assertThat(cclProtocolParams.getPvtCommitteeNoConfidence()).isEqualTo(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence());
        assertThat(cclProtocolParams.getPvtHardForkInitiation()).isEqualTo(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation());
        assertThat(cclProtocolParams.getPvtMotionNoConfidence()).isEqualTo(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence());

        assertThat(cclProtocolParams.getDvtMotionNoConfidence()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence());
        assertThat(cclProtocolParams.getDvtCommitteeNormal()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal());
        assertThat(cclProtocolParams.getDvtCommitteeNoConfidence()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence());
        assertThat(cclProtocolParams.getDvtUpdateToConstitution()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution());
        assertThat(cclProtocolParams.getDvtHardForkInitiation()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation());
        assertThat(cclProtocolParams.getDvtPPNetworkGroup()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup());
        assertThat(cclProtocolParams.getDvtPPEconomicGroup()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup());
        assertThat(cclProtocolParams.getDvtPPTechnicalGroup()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup());
        assertThat(cclProtocolParams.getDvtPPGovGroup()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup());
        assertThat(cclProtocolParams.getDvtTreasuryWithdrawal()).isEqualTo(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal());

        assertThat(cclProtocolParams.getCommitteeMinSize()).isEqualTo(protocolParams.getCommitteeMinSize());
        assertThat(cclProtocolParams.getCommitteeMaxTermLength()).isEqualTo(protocolParams.getCommitteeMaxTermLength());
        assertThat(cclProtocolParams.getGovActionLifetime()).isEqualTo(protocolParams.getGovActionLifetime());
        assertThat(cclProtocolParams.getGovActionDeposit()).isEqualTo(protocolParams.getGovActionDeposit().toString());
        assertThat(cclProtocolParams.getDrepDeposit()).isEqualTo(protocolParams.getDrepDeposit().toString());
        assertThat(cclProtocolParams.getDrepActivity()).isEqualTo(protocolParams.getDrepActivity());
    }

}
