package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.service.model.EpochValidationInput;
import com.bloxbean.cardano.yaci.store.adapot.service.model.ExpectedAdaPot;
import com.bloxbean.cardano.yaci.store.adapot.service.model.RewardsCalcInput;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.StakeSnapshotService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ComponentScan
class EpochRewardCalculationServiceTest {

    @Autowired
    private EpochRewardCalculationService epochRewardCalculationService;

    @Autowired
    private StakeSnapshotService snapshotService;

    @Autowired
    private StoreProperties storeProperties;

    @BeforeEach
    public void setup() {
        storeProperties.setMainnet(true);
    }

//    @Test
    void fetchRewardCalcInputs() throws IOException {

        RewardsCalcInput rewardsCalcInput = epochRewardCalculationService.fetchRewardCalcInputs(480);

        var epochValidationInput = loadEpochValidationInputJson(480);

        System.out.println("Loading of epoch validation input completed");

        System.out.println("Epoch : >>>>>>>>>>>>>> " + rewardsCalcInput.getEpochInfo().getNumber());

        /**
        Map<String, PoolState> rewardsCalcPoolStates = new HashMap<>();
        for (PoolState poolState : rewardsCalcInput.getPoolStates()) {
            rewardsCalcPoolStates.put(poolState.getPoolId(), poolState);
        }

        Map<String, PoolState> epochValidationPoolStates = new HashMap<>();
        for (PoolState poolState : epochValidationInput.getPoolStates()) {
            epochValidationPoolStates.put(poolState.getPoolId(), poolState);
        }

        var rewardCalcPoolIdss = rewardsCalcPoolStates.values().stream()
                .map(poolState -> poolState.getPoolId()).sorted().toList();
        var epochValidationPoolIds = epochValidationPoolStates.values().stream()
                .map(poolState -> poolState.getPoolId()).sorted().toList();

        assertThat(rewardCalcPoolIdss).hasSameElementsAs(epochValidationPoolIds);


        for (String poolId : rewardsCalcPoolStates.keySet()) {
            System.out.println("Comparing pool : " + poolId);
            PoolState rewardsCalcPoolState = rewardsCalcPoolStates.get(poolId);
            PoolState epochValidationPoolState = epochValidationPoolStates.get(poolId);

            //compare all fields of pool state
            assertThat(rewardsCalcPoolState.getPoolId()).isEqualTo(epochValidationPoolState.getPoolId());

//            assertThat(rewardsCalcPoolState.getActiveStake()).isEqualTo(epochValidationPoolState.getActiveStake());
            assertThat(rewardsCalcPoolState.getRewardAddress()).isEqualTo(epochValidationPoolState.getRewardAddress());
            assertThat(rewardsCalcPoolState.getOwners()).hasSameElementsAs(epochValidationPoolState.getOwners());
//            assertThat(rewardsCalcPoolState.getOwnerActiveStake()).isEqualTo(epochValidationPoolState.getOwnerActiveStake());
            assertThat(rewardsCalcPoolState.getPoolFees()).isEqualTo(epochValidationPoolState.getPoolFees());
            assertThat(rewardsCalcPoolState.getMargin()).isEqualTo(epochValidationPoolState.getMargin());
            assertThat(rewardsCalcPoolState.getFixedCost()).isEqualTo(epochValidationPoolState.getFixedCost());
            assertThat(rewardsCalcPoolState.getPledge()).isEqualTo(epochValidationPoolState.getPledge());

            if (rewardsCalcPoolState.getDelegators().size() != epochValidationPoolState.getDelegators().size()) {
                System.out.println("Delegators size mismatch for pool : >>>>> " + poolId +" : reward calc pool size: " + rewardsCalcPoolState.getDelegators().size() + " : epochValidation pool size: " + epochValidationPoolState.getDelegators().size());
                assertThat(rewardsCalcPoolState.getDelegators().stream().map(delegator -> delegator.getStakeAddress()).toList())
                        .hasSameElementsAs(epochValidationPoolState.getDelegators().stream().map(delegator -> delegator.getStakeAddress()).toList());

                assertThat(epochValidationPoolState.getDelegators().stream().map(delegator -> delegator.getStakeAddress()).toList())
                        .hasSameElementsAs(rewardsCalcPoolState.getDelegators().stream().map(delegator -> delegator.getStakeAddress()).toList());

            }
            assertThat(rewardsCalcPoolState.getDelegators().size()).isEqualTo(epochValidationPoolState.getDelegators().size());
            assertThat(rewardsCalcPoolState.getBlockCount()).isEqualTo(epochValidationPoolState.getBlockCount());
            assertThat(rewardsCalcPoolState.getEpoch()).isEqualTo(epochValidationPoolState.getEpoch());
        }
       **/
        assertThat(rewardsCalcInput.getTreasuryOfPreviousEpoch()).isEqualTo(epochValidationInput.getTreasuryOfPreviousEpoch());
        assertThat(rewardsCalcInput.getReservesOfPreviousEpoch()).isEqualTo(epochValidationInput.getReservesOfPreviousEpoch());
//        //compare epoch stake info
//        compareEpochStake(rewardsCalcInput.getEpochInfo().get, epochValidationInput);
//        assertThat(rewardsCalcInput.getEpochInfo().getNumber()).isEqualTo(epochValidationInput.getEpoch());
        assertThat(rewardsCalcInput.getEpochInfo().getActiveStake()).isEqualTo(epochValidationInput.getActiveStake());
        System.out.println(">> Active stake matches");
        assertThat(rewardsCalcInput.getEpochInfo().getBlockCount()).isEqualTo(epochValidationInput.getBlockCount());
        assertThat(rewardsCalcInput.getEpochInfo().getNonOBFTBlockCount()).isEqualTo(epochValidationInput.getNonOBFTBlockCount());
        assertThat(rewardsCalcInput.getEpochInfo().getFees()).isEqualTo(epochValidationInput.getFees());
        assertThat(rewardsCalcInput.getProtocolParameters().getDecentralisation().doubleValue()).isEqualTo(epochValidationInput.getDecentralisation().doubleValue());
        assertThat(rewardsCalcInput.getProtocolParameters().getTreasuryGrowRate()).isEqualTo(epochValidationInput.getTreasuryGrowRate());
        assertThat(rewardsCalcInput.getProtocolParameters().getMonetaryExpandRate()).isEqualTo(epochValidationInput.getMonetaryExpandRate());
        assertThat(rewardsCalcInput.getProtocolParameters().getOptimalPoolCount()).isEqualTo(epochValidationInput.getOptimalPoolCount());
        assertThat(rewardsCalcInput.getProtocolParameters().getPoolOwnerInfluence()).isEqualTo(epochValidationInput.getPoolOwnerInfluence());
        System.out.println(">> Protocol parameters matches");
        assertThat(rewardsCalcInput.getPoolStates().size()).isEqualTo(epochValidationInput.getPoolStates().size());
        var poolIds = rewardsCalcInput.getPoolIds();
        var expectedPoolIds = epochValidationInput.getPoolStates().stream().map(poolState -> poolState.getPoolId()).toList();
        assertThat(poolIds).hasSameElementsAs(expectedPoolIds);

        System.out.println(">> Pool ids matches");
        assertThat(rewardsCalcInput.getMirCertificates().size()).isEqualTo(epochValidationInput.getMirCertificates().size());

        System.out.println(">> Mir certificates matches");
        //Total MIR rewards
        BigInteger totalMirRewards = rewardsCalcInput.getMirCertificates().stream().map(mirCertificate -> mirCertificate.getTotalRewards()).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger expectedTotalMirRewards = epochValidationInput.getMirCertificates().stream().map(mirCertificate -> mirCertificate.getTotalRewards()).reduce(BigInteger.ZERO, BigInteger::add);
        assertThat(totalMirRewards).isEqualTo(expectedTotalMirRewards);

        System.out.println(">> Total MIR rewards matches");


        assertThat(rewardsCalcInput.getRewardAddressesOfRetiredPoolsInEpoch()).hasSameElementsAs(epochValidationInput.getRewardAddressesOfRetiredPoolsInEpoch());
        assertThat(rewardsCalcInput.getRewardAddressesOfRetiredPoolsInEpoch().size()).isEqualTo(epochValidationInput.getRewardAddressesOfRetiredPoolsInEpoch().size());

        System.out.println(">> Reward addresses of retired pools matches");

//        assertThat(rewardsCalcInput.getDeregisteredAccounts()).hasSameElementsAs(epochValidationInput.getDeregisteredAccounts());

        for (String account : rewardsCalcInput.getDeregisteredAccounts()) {
            if (!epochValidationInput.getDeregisteredAccounts().contains(account)) {
                System.out.println("Deregistered account not found : " + account);
            }
        }

        assertThat(rewardsCalcInput.getDeregisteredAccounts().size()).isEqualTo(epochValidationInput.getDeregisteredAccounts().size());

        System.out.println(">> Deregistered accounts matches");

        assertThat(rewardsCalcInput.getLateDeregisteredAccounts()).hasSameElementsAs(epochValidationInput.getLateDeregisteredAccounts());
        assertThat(rewardsCalcInput.getLateDeregisteredAccounts().size()).isEqualTo(epochValidationInput.getLateDeregisteredAccounts().size());

        System.out.println(">> Late Deregistered accounts matches");

        assertThat(rewardsCalcInput.getRegisteredAccountsSinceLastEpoch()).hasSameElementsAs(epochValidationInput.getRegisteredAccountsSinceLastEpoch());
        assertThat(rewardsCalcInput.getRegisteredAccountsSinceLastEpoch().size()).isEqualTo(epochValidationInput.getRegisteredAccountsSinceLastEpoch().size());

        System.out.println(">> Registered accounts since last epoch matches");

        assertThat(rewardsCalcInput.getRegisteredAccountsUntilNow()).hasSameElementsAs(epochValidationInput.getRegisteredAccountsUntilNow());
        assertThat(rewardsCalcInput.getRegisteredAccountsUntilNow().size()).isEqualTo(epochValidationInput.getRegisteredAccountsUntilNow().size());

        System.out.println(">> Registered accounts until now matches");

        assertThat(rewardsCalcInput.getSharedPoolRewardAddressesWithoutReward()).hasSameElementsAs(epochValidationInput.getSharedPoolRewardAddressesWithoutReward());
        assertThat(rewardsCalcInput.getSharedPoolRewardAddressesWithoutReward().size()).isEqualTo(epochValidationInput.getSharedPoolRewardAddressesWithoutReward().size());

        System.out.println(">> Shared pool reward addresses without reward matches");
        assertThat(rewardsCalcInput.getDeregisteredAccountsOnEpochBoundary()).hasSameElementsAs(epochValidationInput.getDeregisteredAccountsOnEpochBoundary());
        assertThat(rewardsCalcInput.getDeregisteredAccountsOnEpochBoundary().size()).isEqualTo(epochValidationInput.getDeregisteredAccountsOnEpochBoundary().size());

        System.out.println(">> All matches");
    }

//    @Test
    void calculateEpochRewards() throws IOException {

        //To clean
        /**
         delete from mainnet.reward where type = 'member' or type = 'leader';
         truncate mainnet.epoch_stake;
//         truncate mainnet.instant_reward;
         update adapot set treasury = null where epoch > 206;
         update adapot set reserves = null where epoch > 206;

         TO clean for 279
         delete from instant_reward where earned_epoch >= 278;

         delete from reward where spendable_epoch >= 279 and (type = 'member' or type = 'leader');
         delete from epoch_stake where epoch >= 278;
         update adapot set treasury = null where epoch >= 279;
         update adapot set reserves = null where epoch >= 279;
         */

        var expectedPots = loadExpectedAdaPotValues();

        for (int i = 496; i <= 506; i++) {

//        for (int i = 265; i <= 286; i++) {
//        for (int i = 279; i <= 315; i++) {
//            var metadata = EventMetadata.builder()
//                    .epochNumber(i)
//                    .slot(0)
//                    .build();
//            instantRewardSnapshotService.takeInstantRewardSnapshot(metadata, i -1);
//            epochRewardCalculationService.calculateAndUpdateEpochRewards(i);

            //Calculate epoch rewards
            var epochCalculationResult = epochRewardCalculationService.calculateEpochRewards(i);

            //Check if the reward calculation is correct
            var expectedAdaPot = expectedPots.get(i);
            if (i >= 209 && expectedAdaPot == null)
                throw new IllegalStateException("Expected AdaPot is null for epoch : " + i);

            System.out.println("Comparing ada pot for epoch : " + i);
            if (expectedAdaPot == null) {
                System.out.println("Treasury or Reserves is null for epoch : " + i);
            } else {
                assertThat(epochCalculationResult.getTreasury()).isEqualTo(expectedAdaPot.getTreasury());
                assertThat(epochCalculationResult.getReserves()).isEqualTo(expectedAdaPot.getReserves());
            }

            //update rewards
            epochRewardCalculationService.updateEpochRewards(i, epochCalculationResult);

            //Now take snapshot
            snapshotService.takeStakeSnapshot(i-1);
        }


        //snapshotService.takeStakeSnapshot(i);
//        epochRewardCalculationService.calculateAndUpdateEpochRewards(215);

    }

    private EpochValidationInput loadEpochValidationInputJson(int epoch) throws IOException {
        String file = String.format("json/epoch-validation-input-%d.json", epoch);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(this.getClass().getClassLoader().getResourceAsStream(file), EpochValidationInput.class);
    }

    private Map<Integer, ExpectedAdaPot> loadExpectedAdaPotValues() throws IOException {
        String file = "json/dbsync_ada_pots.json";
        ObjectMapper objectMapper = new ObjectMapper();
        List<ExpectedAdaPot> pots = objectMapper.readValue(this.getClass().getClassLoader().getResourceAsStream(file), new TypeReference<List<ExpectedAdaPot>>() {});

        Map<Integer, ExpectedAdaPot> potsMap = pots.stream()
                .collect(Collectors.toMap(ExpectedAdaPot::getEpochNo, pot -> pot));

        return potsMap;
    }
}
