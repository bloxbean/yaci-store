package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.service.model.EpochValidationInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.rewards.calculation.domain.PoolState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ComponentScan
class EpochRewardCalculationServiceTest {

    @Autowired
    private EpochRewardCalculationService epochRewardCalculationService;

    @Test
    void fetchRewardCalcInputs() throws IOException {
        var rewardsCalcInput = epochRewardCalculationService.fetchRewardCalcInputs(213);
        var epochValidationInput = loadEpochValidationInputJson(213);


        Map<String, PoolState> rewardsCalcPoolStates = new HashMap<>();
        for (PoolState poolState : rewardsCalcInput.getPoolStates()) {
            rewardsCalcPoolStates.put(poolState.getPoolId(), poolState);
        }

        Map<String, PoolState> epochValidationPoolStates = new HashMap<>();
        for (PoolState poolState : epochValidationInput.getPoolStates()) {
            epochValidationPoolStates.put(poolState.getPoolId(), poolState);
        }

        for (String poolId : rewardsCalcPoolStates.keySet()) {
            PoolState rewardsCalcPoolState = rewardsCalcPoolStates.get(poolId);
            PoolState epochValidationPoolState = epochValidationPoolStates.get(poolId);
            //compare all fields of pool state
            assertThat(rewardsCalcPoolState.getPoolId()).isEqualTo(epochValidationPoolState.getPoolId());
            assertThat(rewardsCalcPoolState.getActiveStake()).isEqualTo(epochValidationPoolState.getActiveStake());
            assertThat(rewardsCalcPoolState.getRewardAddress()).isEqualTo(epochValidationPoolState.getRewardAddress());
            assertThat(rewardsCalcPoolState.getOwners()).hasSameElementsAs(epochValidationPoolState.getOwners());
            assertThat(rewardsCalcPoolState.getOwnerActiveStake()).isEqualTo(epochValidationPoolState.getOwnerActiveStake());
            assertThat(rewardsCalcPoolState.getPoolFees()).isEqualTo(epochValidationPoolState.getPoolFees());
            assertThat(rewardsCalcPoolState.getMargin()).isEqualTo(epochValidationPoolState.getMargin());
            assertThat(rewardsCalcPoolState.getFixedCost()).isEqualTo(epochValidationPoolState.getFixedCost());
            assertThat(rewardsCalcPoolState.getPledge()).isEqualTo(epochValidationPoolState.getPledge());
            assertThat(rewardsCalcPoolState.getDelegators().size()).isEqualTo(epochValidationPoolState.getDelegators().size());
            assertThat(rewardsCalcPoolState.getBlockCount()).isEqualTo(epochValidationPoolState.getBlockCount());
            assertThat(rewardsCalcPoolState.getEpoch()).isEqualTo(epochValidationPoolState.getEpoch());
        }

        assertThat(rewardsCalcInput.getTreasuryOfPreviousEpoch()).isEqualTo(epochValidationInput.getTreasuryOfPreviousEpoch());
        assertThat(rewardsCalcInput.getReservesOfPreviousEpoch()).isEqualTo(epochValidationInput.getReservesOfPreviousEpoch());
        assertThat(rewardsCalcInput.getEpochInfo().getActiveStake()).isEqualTo(epochValidationInput.getActiveStake());
        assertThat(rewardsCalcInput.getEpochInfo().getBlockCount()).isEqualTo(epochValidationInput.getBlockCount());
        assertThat(rewardsCalcInput.getEpochInfo().getNonOBFTBlockCount()).isEqualTo(epochValidationInput.getNonOBFTBlockCount());
        assertThat(rewardsCalcInput.getEpochInfo().getFees()).isEqualTo(epochValidationInput.getFees());
        assertThat(rewardsCalcInput.getProtocolParameters().getDecentralisation().doubleValue()).isEqualTo(epochValidationInput.getDecentralisation().doubleValue());
        assertThat(rewardsCalcInput.getProtocolParameters().getTreasuryGrowRate()).isEqualTo(epochValidationInput.getTreasuryGrowRate());
        assertThat(rewardsCalcInput.getProtocolParameters().getMonetaryExpandRate()).isEqualTo(epochValidationInput.getMonetaryExpandRate());
        assertThat(rewardsCalcInput.getProtocolParameters().getOptimalPoolCount()).isEqualTo(epochValidationInput.getOptimalPoolCount());
        assertThat(rewardsCalcInput.getProtocolParameters().getPoolOwnerInfluence()).isEqualTo(epochValidationInput.getPoolOwnerInfluence());

        assertThat(rewardsCalcInput.getPoolStates().size()).isEqualTo(epochValidationInput.getPoolStates().size());
        var poolIds = rewardsCalcInput.getPoolIds();
        var expectedPoolIds = epochValidationInput.getPoolStates().stream().map(poolState -> poolState.getPoolId()).toList();
        assertThat(poolIds).hasSameElementsAs(expectedPoolIds);

        assertThat(rewardsCalcInput.getMirCertificates().size()).isEqualTo(epochValidationInput.getMirCertificates().size());
        assertThat(rewardsCalcInput.getRewardAddressesOfRetiredPoolsInEpoch().size()).isEqualTo(epochValidationInput.getRewardAddressesOfRetiredPoolsInEpoch().size());
        assertThat(rewardsCalcInput.getDeregisteredAccounts().size()).isEqualTo(epochValidationInput.getDeregisteredAccounts().size());
        assertThat(rewardsCalcInput.getLateDeregisteredAccounts().size()).isEqualTo(epochValidationInput.getLateDeregisteredAccounts().size());

        //stake1uyqkpllunxtg98w0em0l2pgyp4etf6mnh2h9qzzr8laucdsehed4k
        assertThat(rewardsCalcInput.getRegisteredAccountsSinceLastEpoch()).hasSameElementsAs(epochValidationInput.getRegisteredAccountsSinceLastEpoch());
        assertThat(rewardsCalcInput.getRegisteredAccountsSinceLastEpoch().size()).isEqualTo(epochValidationInput.getRegisteredAccountsSinceLastEpoch().size());

        //stake1uyqkpllunxtg98w0em0l2pgyp4etf6mnh2h9qzzr8laucdsehed4k
        assertThat(rewardsCalcInput.getRegisteredAccountsUntilNow()).hasSameElementsAs(epochValidationInput.getRegisteredAccountsUntilNow());
        assertThat(rewardsCalcInput.getRegisteredAccountsUntilNow().size()).isEqualTo(epochValidationInput.getRegisteredAccountsUntilNow().size());

        assertThat(rewardsCalcInput.getSharedPoolRewardAddressesWithoutReward().size()).isEqualTo(epochValidationInput.getSharedPoolRewardAddressesWithoutReward().size());
        assertThat(rewardsCalcInput.getDeregisteredAccountsOnEpochBoundary().size()).isEqualTo(epochValidationInput.getDeregisteredAccountsOnEpochBoundary().size());

    }

    @Test
    void calculateEpochRewards() {
        epochRewardCalculationService.calculateAndUpdateEpochRewards(213);
    }

    private EpochValidationInput loadEpochValidationInputJson(int epoch) throws IOException {
        String file = String.format("json/epoch-validation-input-%d.json", epoch);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(this.getClass().getClassLoader().getResourceAsStream(file), EpochValidationInput.class);
    }
}
