package com.bloxbean.cardano.yaci.store.epoch.mapper;

import com.bloxbean.cardano.yaci.core.model.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.core.model.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.types.NonNegativeInterval;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;
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
                .poolPledgeInfluence(new NonNegativeInterval(BigInteger.valueOf(30), BigInteger.valueOf(100)))
                .expansionRate(UnitInterval.fromString("40/100"))
                .treasuryGrowthRate(UnitInterval.fromString("50/100"))
                .decentralisationParam(UnitInterval.fromString("60/100"))
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
                        .pvtCommitteeNormal(UnitInterval.fromString("10/100"))
                        .pvtCommitteeNoConfidence(UnitInterval.fromString("20/100"))
                        .pvtHardForkInitiation(UnitInterval.fromString("30/100"))
                        .pvtMotionNoConfidence(UnitInterval.fromString("40/100"))
                    .build()
                )
                .drepVotingThresholds(DrepVoteThresholds.builder()
                        .dvtMotionNoConfidence(UnitInterval.fromString("10/100"))
                        .dvtCommitteeNormal(UnitInterval.fromString("20/100"))
                        .dvtCommitteeNoConfidence(UnitInterval.fromString("30/100"))
                        .dvtUpdateToConstitution(UnitInterval.fromString("40/100"))
                        .dvtHardForkInitiation(UnitInterval.fromString("50/100"))
                        .dvtPPNetworkGroup(UnitInterval.fromString("60/100"))
                        .dvtPPEconomicGroup(UnitInterval.fromString("70/100"))
                        .dvtPPTechnicalGroup(UnitInterval.fromString("80/100"))
                        .dvtPPGovGroup(UnitInterval.fromString("90/100"))
                        .dvtTreasuryWithdrawal(UnitInterval.fromString("10/100"))
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
                .poolPledgeInfluence(new NonNegativeInterval(BigInteger.valueOf(30), BigInteger.valueOf(100)))
                .expansionRate(UnitInterval.fromString("40/100"))
                .treasuryGrowthRate(UnitInterval.fromString("50/100"))
                .decentralisationParam(UnitInterval.fromString("60/100"))
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
                        .pvtCommitteeNormal(UnitInterval.fromString("10/100"))
                        .pvtCommitteeNoConfidence(UnitInterval.fromString("20/100"))
                        .pvtHardForkInitiation(UnitInterval.fromString("30/100"))
                        .pvtMotionNoConfidence(UnitInterval.fromString("40/100"))
                        .build()
                )
                .drepVotingThresholds(com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds.builder()
                        .dvtMotionNoConfidence(UnitInterval.fromString("10/100"))
                        .dvtCommitteeNormal(UnitInterval.fromString("20/100"))
                        .dvtCommitteeNoConfidence(UnitInterval.fromString("30/100"))
                        .dvtUpdateToConstitution(UnitInterval.fromString("40/100"))
                        .dvtHardForkInitiation(UnitInterval.fromString("50/100"))
                        .dvtPPNetworkGroup(UnitInterval.fromString("60/100"))
                        .dvtPPEconomicGroup(UnitInterval.fromString("70/100"))
                        .dvtPPTechnicalGroup(UnitInterval.fromString("80/100"))
                        .dvtPPGovGroup(UnitInterval.fromString("90/100"))
                        .dvtTreasuryWithdrawal(UnitInterval.fromString("10/100"))
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
        assertThat(cclProtocolParams.getA0()).isEqualByComparingTo(safeRatio(protocolParams.getPoolPledgeInfluence()));
        assertThat(cclProtocolParams.getRho()).isEqualByComparingTo(safeRatio(protocolParams.getExpansionRate()));
        assertThat(cclProtocolParams.getTau()).isEqualByComparingTo(safeRatio(protocolParams.getTreasuryGrowthRate()));
        assertThat(cclProtocolParams.getDecentralisationParam()).isEqualByComparingTo(safeRatio(protocolParams.getDecentralisationParam()));
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

        assertThat(cclProtocolParams.getPvtCommitteeNormal()).isEqualByComparingTo(safeRatio(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal()));
        assertThat(cclProtocolParams.getPvtCommitteeNoConfidence()).isEqualByComparingTo(safeRatio(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence()));
        assertThat(cclProtocolParams.getPvtHardForkInitiation()).isEqualByComparingTo(safeRatio(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation()));
        assertThat(cclProtocolParams.getPvtMotionNoConfidence()).isEqualByComparingTo(safeRatio(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence()));

        assertThat(cclProtocolParams.getDvtMotionNoConfidence()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence()));
        assertThat(cclProtocolParams.getDvtCommitteeNormal()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal()));
        assertThat(cclProtocolParams.getDvtCommitteeNoConfidence()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence()));
        assertThat(cclProtocolParams.getDvtUpdateToConstitution()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution()));
        assertThat(cclProtocolParams.getDvtHardForkInitiation()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation()));
        assertThat(cclProtocolParams.getDvtPPNetworkGroup()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup()));
        assertThat(cclProtocolParams.getDvtPPEconomicGroup()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup()));
        assertThat(cclProtocolParams.getDvtPPTechnicalGroup()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup()));
        assertThat(cclProtocolParams.getDvtPPGovGroup()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup()));
        assertThat(cclProtocolParams.getDvtTreasuryWithdrawal()).isEqualByComparingTo(safeRatio(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal()));

        assertThat(cclProtocolParams.getCommitteeMinSize()).isEqualTo(protocolParams.getCommitteeMinSize());
        assertThat(cclProtocolParams.getCommitteeMaxTermLength()).isEqualTo(protocolParams.getCommitteeMaxTermLength());
        assertThat(cclProtocolParams.getGovActionLifetime()).isEqualTo(protocolParams.getGovActionLifetime());
        assertThat(cclProtocolParams.getGovActionDeposit()).isEqualTo(protocolParams.getGovActionDeposit().toString());
        assertThat(cclProtocolParams.getDrepDeposit()).isEqualTo(protocolParams.getDrepDeposit().toString());
        assertThat(cclProtocolParams.getDrepActivity()).isEqualTo(protocolParams.getDrepActivity());
    }

}
