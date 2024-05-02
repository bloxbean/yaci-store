package com.bloxbean.cardano.yaci.store.reward;

import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.service.ProtocolParamService;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.DepositsCalculation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RewardCalculation {
    private final EraService eraService;
    private final BlockStorageReader blockStorageReader;
    private final RewardStorageReader rewardStorageReader;
    private final AdaPotService adaPotService;
    private final ProtocolParamService protocolParamService;

    private Integer shelleyStartEpoch;

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void calculateRewards() {
        if (getShelleyStartEpoch() == null) {
            return;
        }

        var epoch = blockStorageReader.findRecentBlock()
                .map(Block::getEpochNumber)
                .orElse(0);

        if (epoch == 0)
            return;

        if (epoch < getShelleyStartEpoch() + 3)
            return;

        var rewardCalculationEpoch = rewardStorageReader.getLastRewardCalculationEpoch(RewardType.member)
                .map(e -> e + 1)
                .orElse(getShelleyStartEpoch() + 3);

        if (rewardCalculationEpoch > epoch) {
            log.info("Rewards for epoch : {} is already calculated", epoch);
            return;
        }

        int blockProductionEpoch = rewardCalculationEpoch - 1;
        int snapshotEpoch = blockProductionEpoch - 2;

        log.info(">>> Start reward calculation for epoch : {}, block production epoch: {}, snapshot epoch: {}", rewardCalculationEpoch, blockProductionEpoch, snapshotEpoch);


    }

    private void computeEpochRewards(int calculationEpoch, int blockProductionEpoch, int snapshotEpoch) {
        double treasuryGrowthRate = 0.2; //TODO: Get from protocol parameters
        double monetaryExpandRate = 0.003; //TODO: Get from protocol parameters
        double decentralizationParameter = 1; //TODO: Get from protocol parameters

        var adaPotsForPreviousEpoch = adaPotService.getAdaPot(blockProductionEpoch);
        BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
        int totalBlocksInEpoch = 0;

        var protocolParams = protocolParamService.getProtocolParam(snapshotEpoch)
                .orElse(null);


//        if (protocolParams == null) {
//            log.error("Reward calculation can't be done. Protocol parameters not found for epoch : {}", snapshotEpoch);
//            return;
//        }
//
//        Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);
//
//        // Step 1: Get Pool information of current epoch
//        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210
//
//        PoolHistory poolHistoryCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch);
//
//        if(poolHistoryCurrentEpoch == null) {
//            return PoolRewardCalculationResult.builder().poolId(poolId).epoch(epoch).poolReward(BigInteger.ZERO).build();
//        }
//
//        BigInteger activeStakeInEpoch = BigInteger.ZERO;
//        if (epochInfo.getActiveStake() != null) {
//            activeStakeInEpoch = epochInfo.getActiveStake();
//        }
//
//        int totalBlocksInEpoch = epochInfo.getBlockCount();
//
//        if (epoch > 212 && epoch < 255) {
//            totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
//        }
//
//        // Step 10 a: Check if pool reward address or member stake addresses have been unregistered before
//        List<String> stakeAddresses = new ArrayList<>();
//        stakeAddresses.add(poolHistoryCurrentEpoch.getRewardAddress());
//        stakeAddresses.addAll(poolHistoryCurrentEpoch.getDelegators().stream().map(Delegator::getStakeAddress).toList());
//
//        //List<AccountUpdate> accountUpdates = dataProvider.getAccountUpdatesUntilEpoch(stakeAddresses, epoch - 1);
//
//        accountUpdates = accountUpdates.stream()
//                .filter(update -> stakeAddresses.contains(update.getStakeAddress())).toList();
//
//        BigInteger poolOperatorRewardOutlier = correctOutliers(poolId, epoch);
//
//        // shelley-delegation.pdf 5.5.3
//        //      "[...]the relative stake of the pool owner(s) (the amount of ada
//        //      pledged during pool registration)"
//        return calculatePoolRewardInEpoch(poolId, poolHistoryCurrentEpoch,
//                totalBlocksInEpoch, protocolParameters,
//                adaInCirculation, activeStakeInEpoch, stakePoolRewardsPot,
//                poolHistoryCurrentEpoch.getOwnerActiveStake(), poolHistoryCurrentEpoch.getOwners(),
//                accountUpdates, poolOperatorRewardOutlier);
    }

    private Integer getShelleyStartEpoch() {
        if (shelleyStartEpoch == null)
            shelleyStartEpoch = eraService.getFirstNonByronEpoch().orElse(null);

        return shelleyStartEpoch;
    }
}
