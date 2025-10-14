package com.bloxbean.cardano.yaci.store.cip139.protocolparameters.dto;

import com.bloxbean.cardano.yaci.core.types.NonNegativeInterval;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

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

        // Validate all fields are correctly mapped
        assertEquals(String.valueOf(100), protocolParametersDto.getMinfeeA());
        assertEquals(String.valueOf(200), protocolParametersDto.getMinfeeB());
        assertEquals(1000, protocolParametersDto.getMaxBlockBodySize());
        assertEquals(2000, protocolParametersDto.getMaxTxSize());
        assertEquals(3000, protocolParametersDto.getMaxBlockHeaderSize());
        assertEquals(BigInteger.valueOf(4000).toString(), protocolParametersDto.getKeyDeposit());
        assertEquals(BigInteger.valueOf(5000).toString(), protocolParametersDto.getPoolDeposit());
        assertEquals(100, protocolParametersDto.getMaxEpoch());
        assertEquals(String.valueOf(200), protocolParametersDto.getNOpt());
        
        // Validate UnitInterval fields
        assertNotNull(protocolParametersDto.getPoolPledgeInfluence());
        assertEquals("30/100", protocolParametersDto.getPoolPledgeInfluence().getNumerator() + "/" + protocolParametersDto.getPoolPledgeInfluence().getDenominator());
        
        assertNotNull(protocolParametersDto.getExpansionRate());
        assertEquals("40/100", protocolParametersDto.getExpansionRate().getNumerator() + "/" + protocolParametersDto.getExpansionRate().getDenominator());
        
        assertNotNull(protocolParametersDto.getTreasuryGrowthRate());
        assertEquals("50/100", protocolParametersDto.getTreasuryGrowthRate().getNumerator() + "/" + protocolParametersDto.getTreasuryGrowthRate().getDenominator());
        
        assertNotNull(protocolParametersDto.getD());
        assertEquals("60/100", protocolParametersDto.getD().getNumerator() + "/" + protocolParametersDto.getD().getDenominator());
        
        assertEquals(2, protocolParametersDto.getProtocolVersion().major());
        assertEquals(3, protocolParametersDto.getProtocolVersion().minor());
        assertEquals(BigInteger.valueOf(6000).toString(), protocolParametersDto.getMinPoolCost());
        assertEquals(BigInteger.valueOf(7000).toString(), protocolParametersDto.getAdaPerUtxoByte());
        assertEquals(new ProtocolParametersDto.Unit("8000", "9000"), protocolParametersDto.getMaxTxExUnits());
        assertEquals(new ProtocolParametersDto.Unit("10000", "11000"), protocolParametersDto.getMaxBlockExUnits());
        assertEquals(8000, protocolParametersDto.getMaxValueSize());
        assertEquals(10000, protocolParametersDto.getMaxCollateralInputs());

        // Validate PoolVotingThresholds
        assertNotNull(protocolParametersDto.getPoolVotingThresholds());
        List<UnitInterval> poolThresholds = protocolParametersDto.getPoolVotingThresholds().items();
        // Order is: [pvtMotionNoConfidence, pvtCommitteeNormal, pvtCommitteeNoConfidence, pvtHardForkInitiation, ...]
        assertEquals("40/100", poolThresholds.get(0).getNumerator() + "/" + poolThresholds.get(0).getDenominator());  // pvtMotionNoConfidence
        assertEquals("10/100", poolThresholds.get(1).getNumerator() + "/" + poolThresholds.get(1).getDenominator());  // pvtCommitteeNormal
        assertEquals("20/100", poolThresholds.get(2).getNumerator() + "/" + poolThresholds.get(2).getDenominator());  // pvtCommitteeNoConfidence
        assertEquals("30/100", poolThresholds.get(3).getNumerator() + "/" + poolThresholds.get(3).getDenominator());  // pvtHardForkInitiation

        // Validate DrepVoteThresholds
        assertNotNull(protocolParametersDto.getDrepVotingThresholds());
        List<UnitInterval> drepThresholds = protocolParametersDto.getDrepVotingThresholds().items();
        assertEquals("10/100", drepThresholds.get(0).getNumerator() + "/" + drepThresholds.get(0).getDenominator());
        assertEquals("20/100", drepThresholds.get(1).getNumerator() + "/" + drepThresholds.get(1).getDenominator());
        assertEquals("30/100", drepThresholds.get(2).getNumerator() + "/" + drepThresholds.get(2).getDenominator());
        assertEquals("40/100", drepThresholds.get(3).getNumerator() + "/" + drepThresholds.get(3).getDenominator());
        assertEquals("50/100", drepThresholds.get(4).getNumerator() + "/" + drepThresholds.get(4).getDenominator());
        assertEquals("60/100", drepThresholds.get(5).getNumerator() + "/" + drepThresholds.get(5).getDenominator());
        assertEquals("70/100", drepThresholds.get(6).getNumerator() + "/" + drepThresholds.get(6).getDenominator());
        assertEquals("80/100", drepThresholds.get(7).getNumerator() + "/" + drepThresholds.get(7).getDenominator());
        assertEquals("90/100", drepThresholds.get(8).getNumerator() + "/" + drepThresholds.get(8).getDenominator());
        assertEquals("10/100", drepThresholds.get(9).getNumerator() + "/" + drepThresholds.get(9).getDenominator());
        
        // Validate remaining fields
        assertEquals(String.valueOf(1000), protocolParametersDto.getCommitteeMinSize());
        assertEquals(10, protocolParametersDto.getCommitteeMaxTermLength());
        assertEquals(5000, protocolParametersDto.getGovActionLifetime());
        assertEquals(BigInteger.valueOf(6000).toString(), protocolParametersDto.getGovActionDeposit());
        assertEquals(BigInteger.valueOf(10000).toString(), protocolParametersDto.getDrepDeposit());
        assertEquals(7000, protocolParametersDto.getDrepActivity());
    }
}