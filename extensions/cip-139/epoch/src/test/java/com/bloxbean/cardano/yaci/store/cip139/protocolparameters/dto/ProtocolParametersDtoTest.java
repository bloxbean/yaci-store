package com.bloxbean.cardano.yaci.store.cip139.protocolparameters.dto;

import com.bloxbean.cardano.yaci.core.types.NonNegativeInterval;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolParametersDtoTest {

    @Test
    void fromDomain() {

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

        ProtocolParametersDto protocolParametersDto = ProtocolParametersDto.fromDomain(protocolParams);

    }
}