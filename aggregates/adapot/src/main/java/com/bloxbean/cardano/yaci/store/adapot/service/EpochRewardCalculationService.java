package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.service.model.RewardsCalcInput;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.adapot.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.service.ProtocolParamService;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.EpochCalculation;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.*;
import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.EXPECTED_SLOTS_PER_EPOCH;

@Component
@RequiredArgsConstructor
@Slf4j
public class EpochRewardCalculationService {
    private final AdaPotService adaPotService;
    private final AdaPotStorage adaPotStorage;
    private final ProtocolParamService protocolParamService;
    private final EpochInfoService epochInfoService;
    private final PoolStorage poolStorage;
    private final PoolStorageReader poolStorageReader;
    private final BlockInfoService blockInfoService;
    private final PoolStateService poolStateService;
    private final EraService eraService;
    private final StakeRegistrationService stakeRegistrationService;
    private final SharedPoolRewardAddresses sharedPoolRewardAddresses; //TOD -- remove later

    private final RewardStorage rewardStorage;
    private final RewardStorageReader rewardStorageReader;

    @Transactional(readOnly = true)
    public RewardsCalcInput fetchRewardCalcInputs(int epoch) {
        //Calculating rewards at epoch "epoch"
        //Block producing epoch is epoch - 1 (Fee + MIR + DeRegistration)
        //Stake snapshot epoch is epoch - 2 (Stake, Protocol Parameters)

        if (epoch <= MAINNET_SHELLEY_START_EPOCH) {
            return null;
        }

        long overallStart = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        log.debug("Start obtaining the epoch data");

        var adaPotOptional = adaPotStorage.findByEpoch(epoch - 1); //adaPotService.getAdaPot(epoch - 1);
        if (adaPotOptional.isEmpty()) {
            log.error("AdaPot not found for epoch " + (epoch - 1));
            log.error("Skipping reward calculation for epoch " + epoch);
            return null;
        }

        AdaPot adaPot = adaPotOptional.get();
        AdaPots adaPotsForPreviousEpoch = AdaPots.builder()
                .epoch(adaPot.getEpoch())
                .treasury(adaPot.getTreasury())
                .reserves(adaPot.getReserves())
                .rewards(adaPot.getRewards())
                .deposits(adaPot.getDeposits())
                .adaInCirculation(adaPot.getUtxo())
                .fees(adaPot.getFees())
                .build();

        ProtocolParams protocolParams;
        if (epoch == MAINNET_SHELLEY_START_EPOCH + 1) { // If this is first epoch after shelley start epoch
            protocolParams = protocolParamService.getProtocolParam(epoch - 1)
                    .orElseThrow(() -> new RuntimeException("Protocol parameters not found for epoch " + (epoch - 2)));

        } else {
            protocolParams = protocolParamService.getProtocolParam(epoch - 2)
                    .orElseThrow(() -> new RuntimeException("Protocol parameters not found for epoch " + (epoch - 2)));
        }

        ProtocolParameters rewardProtocolParameters = ProtocolParameters.builder()
                .decentralisation(protocolParams.getDecentralisationParam() != null?
                        protocolParams.getDecentralisationParam() : BigDecimal.ZERO)
                .treasuryGrowRate(protocolParams.getTreasuryGrowthRate())
                .monetaryExpandRate(protocolParams.getExpansionRate())
                .optimalPoolCount(protocolParams.getNOpt())
                .poolOwnerInfluence(protocolParams.getPoolPledgeInfluence())
                .build();

        //Get epoch info
        var epochInfo = epochInfoService.getEpochInfo(epoch - 2) //should we do epoch - 2 as it's already checking active epoch
                .orElse(null);
             //   .orElseThrow(() -> new RuntimeException("Epoch info not found for epoch " + epoch)); //TODO: Handle this

        //Get reward addressed of retired pools
        List<Pool> retiringPools = poolStorage.findRetiringPools(epoch);
        HashSet<String> rewardAddressesOfRetiredPoolsInEpoch = new HashSet<>();
        for (Pool pool : retiringPools) {
//            var delegatedAddresses = epochStakeStorage.getAllActiveStakesByEpochAndPool(epoch, pool.getPoolId())
//                    .stream()
//                    .map(epochStake -> epochStake.getAddress()).toList();
            List<PoolDetails> poolDetails = poolStorageReader.getLatestPoolUpdateDetails(List.of(pool.getPoolId()), epoch - 1); //-1 or not //TODO ??
            if(poolDetails == null || poolDetails.isEmpty()) {
                log.info("Pool details not found for pool : " + pool.getPoolId() + " in epoch " + epoch);
                continue;
            }

            rewardAddressesOfRetiredPoolsInEpoch.add(poolDetails.get(0).getRewardAccount());
//            poolStorageReader.getPoolDetails(List.of(pool.getPoolId()), epoch).stream().findFirst().ifPresent(poolDetails -> {
//                rewardAddressesOfRetiredPoolsInEpoch.add(poolDetails.getRewardAccount());
//            })
          //  rewardAddressesOfRetiredPoolsInEpoch.addAll(delegatedAddresses);
        }

        //Get MIR certificates totals by pot ..stability window ??
        var totalRewardsTreasury = rewardStorageReader.findTotalInstanceRewardByEarnedEpochAndType(epoch - 1, InstantRewardType.treasury);
//        var totalRewardsTreasury = mirStorageReader.findMirPotAmountByEpoch(epoch-1, MirPot.TREASURY);
        var mirTreasuryCertificate = MirCertificate.builder()
                        .pot(org.cardanofoundation.rewards.calculation.enums.MirPot.TREASURY)
                                .totalRewards(totalRewardsTreasury != null ? totalRewardsTreasury : BigInteger.ZERO)
                                        .build();

        var totalRewardsReserves = rewardStorageReader.findTotalInstanceRewardByEarnedEpochAndType(epoch - 1, InstantRewardType.reserves);
//        var totalRewardsReserves = mirStorageReader.findMirPotAmountByEpoch(epoch-1, MirPot.RESERVES);
        var mirCertificateReserves = MirCertificate.builder()
                .pot(org.cardanofoundation.rewards.calculation.enums.MirPot.RESERVES)
                .totalRewards(totalRewardsReserves != null ? totalRewardsReserves : BigInteger.ZERO)
                .build();

        List<MirCertificate> mirCertificates = new ArrayList<>();
        if (mirTreasuryCertificate.getTotalRewards().compareTo(BigInteger.ZERO) > 0) {
            mirCertificates.add(mirTreasuryCertificate);
        }
        if (mirCertificateReserves.getTotalRewards().compareTo(BigInteger.ZERO) > 0) {
            mirCertificates.add(mirCertificateReserves);
        }

        //Blocks made by pools in epoch
        List<PoolBlock> blocksMadeByPoolsInEpoch = blockInfoService.getPoolBlockCount(epoch - 2) //active epoch //TODO --  check epoch value
                .stream().map(poolBlock -> PoolBlock.builder()
                        .poolId(poolBlock.getPoolId())
                        .blockCount(poolBlock.getBlocks())
                        .build()).toList();

        //hex pool ids
        List<String> poolIds = blocksMadeByPoolsInEpoch.stream().map(PoolBlock::getPoolId).distinct().toList();

        List<PoolState> poolStates = poolStateService.getHistoryOfAllPoolsInEpoch(epoch -2, blocksMadeByPoolsInEpoch);
        List<String> bech32PoolIds = poolIds.stream().map(poolId -> PoolUtil.getBech32PoolId(poolId)).toList();

        System.out.println("Pool ids\n" + JsonUtil.getPrettyJson(bech32PoolIds.stream().sorted().toList()));
        HashSet<String> deregisteredAccounts;
        HashSet<String> deregisteredAccountsOnEpochBoundary;
        HashSet<String> lateDeregisteredAccounts = new HashSet<>();
        if (epoch-2 < MAINNET_VASIL_HARDFORK_EPOCH) { //epoch - 2 ??  //Reward calculation epoch
            //convert to absolute slot
            //add epoch slot to stake_registration
            long epochRandomStabilizationWindowAbsoluteSlot = eraService.getShelleyAbsoluteSlot(epoch - 1, (int)RANDOMNESS_STABILISATION_WINDOW);//TODO epoch -2?
            var deregAccList = stakeRegistrationService.getDeregisteredAccountsInEpoch(epoch - 1, epochRandomStabilizationWindowAbsoluteSlot); //TODO  RANDOMNESS_STABILISATION_WINDOW = 48 ?
            deregisteredAccounts = new HashSet<>(deregAccList);

            long epochBoundaryAbsoluteSlot = eraService.getShelleyAbsoluteSlot(epoch - 1, (int)EXPECTED_SLOTS_PER_EPOCH); //TODO epoch -2?
            var deregAccListEpochBoundary = stakeRegistrationService.getDeregisteredAccountsInEpoch(epoch - 1, epochBoundaryAbsoluteSlot);
            deregisteredAccountsOnEpochBoundary = new HashSet<>(deregAccListEpochBoundary);

            lateDeregisteredAccounts = deregisteredAccountsOnEpochBoundary.stream().filter(account -> !deregisteredAccounts.contains(account)).collect(Collectors.toCollection(HashSet::new));
        } else {
            long epochBoundaryAbsoluteSlot = eraService.getShelleyAbsoluteSlot(epoch - 1, (int)EXPECTED_SLOTS_PER_EPOCH);
            var deregAccList = stakeRegistrationService.getDeregisteredAccountsInEpoch(epoch - 1, epochBoundaryAbsoluteSlot);
            deregisteredAccounts = new HashSet<>(deregAccList);
            deregisteredAccountsOnEpochBoundary = deregisteredAccounts;
        }

        HashSet<String> sharedPoolRewardAddressesWithoutReward = new HashSet<>();
        if (epoch - 2 < MAINNET_ALLEGRA_HARDFORK_EPOCH) {
            sharedPoolRewardAddressesWithoutReward = new HashSet<>(sharedPoolRewardAddresses.getSharedPoolRewardAddressesWithoutReward(epoch));
            //TODO -- Hardcode for now
           // sharedPoolRewardAddressesWithoutReward = dataProvider.findSharedPoolRewardAddressWithoutReward(epoch - 2);
        }
        HashSet<String> poolRewardAddresses = poolStates.stream().map(PoolState::getRewardAddress).collect(Collectors.toCollection(HashSet::new));
        poolRewardAddresses.addAll(rewardAddressesOfRetiredPoolsInEpoch);

        System.out.println("Pool reward addresses: \n " + JsonUtil.getPrettyJson(poolRewardAddresses.stream().sorted().toList()));

        long stabilityWindow = RANDOMNESS_STABILISATION_WINDOW;
        // Since the Vasil hard fork, the unregistered accounts will not filter out before the
        // rewards calculation starts (at the stability window). They will be filtered out on the
        // epoch boundary when the reward update will be applied.
        if (epoch - 2 >= MAINNET_VASIL_HARDFORK_EPOCH) {
            stabilityWindow = EXPECTED_SLOTS_PER_EPOCH;
        }

        int lastEpoch = epoch - 1;
        long startSlotOfEpochLastEpoch = eraService.getShelleyAbsoluteSlot(lastEpoch, 0);
        long lastEpochStabilityWindowAbsoluteSlot = startSlotOfEpochLastEpoch + stabilityWindow;
       // long lastEpochStabilityWindowAbsoluteSlot = startSlotOfEpoch - stabilityWindow;
       // long lastEpochStabilityWindowAbsoluteSlot = eraService.getShelleyAbsoluteSlot(lastEpoch, (int)stabilityWindow);
        log.info("Last epoch stability window slot : " + lastEpoch);
        log.info("Last epoch stability window slot : " + lastEpochStabilityWindowAbsoluteSlot);
        var registeredAccountsUntilLastEpochList = stakeRegistrationService.getRegisteredAccountsUntilEpoch(lastEpoch, poolRewardAddresses, lastEpochStabilityWindowAbsoluteSlot);
        HashSet<String> registeredAccountsSinceLastEpoch = new HashSet<>(registeredAccountsUntilLastEpochList);

        System.out.println(JsonUtil.getPrettyJson(registeredAccountsSinceLastEpoch));

        long currentEpochStabilityWindowAbsoluteSlot = eraService.getShelleyAbsoluteSlot(epoch, (int)stabilityWindow);
        var registeredAccountsUntilNowList = stakeRegistrationService.getRegisteredAccountsUntilEpoch(epoch, poolRewardAddresses, currentEpochStabilityWindowAbsoluteSlot);
        HashSet<String> registeredAccountsUntilNow = new HashSet<>(registeredAccountsUntilNowList);

        //Remove
//        registeredAccountsSinceLastEpoch.add("stake1uyqkpllunxtg98w0em0l2pgyp4etf6mnh2h9qzzr8laucdsehed4k");
//        registeredAccountsUntilNow.add("stake1uyqkpllunxtg98w0em0l2pgyp4etf6mnh2h9qzzr8laucdsehed4k");

        RewardsCalcInput rewardsCalcInput = RewardsCalcInput.builder()
                .epoch(epoch)
                .treasuryOfPreviousEpoch(adaPotsForPreviousEpoch.getTreasury())
                .reservesOfPreviousEpoch(adaPotsForPreviousEpoch.getReserves())
                .protocolParameters(rewardProtocolParameters)
                .epochInfo(epochInfo)
                .rewardAddressesOfRetiredPoolsInEpoch(rewardAddressesOfRetiredPoolsInEpoch)
                .deregisteredAccounts(deregisteredAccounts)
                .lateDeregisteredAccounts(lateDeregisteredAccounts)
                .registeredAccountsSinceLastEpoch(registeredAccountsSinceLastEpoch)
                .registeredAccountsUntilNow(registeredAccountsUntilNow)
                .sharedPoolRewardAddressesWithoutReward(sharedPoolRewardAddressesWithoutReward)
                .deregisteredAccountsOnEpochBoundary(deregisteredAccountsOnEpochBoundary)
                .poolIds(bech32PoolIds)
                .poolStates(poolStates)
                .mirCertificates(mirCertificates)
                .build();

//        start = System.currentTimeMillis();
//        var epochCalculationResult = EpochCalculation.calculateEpochRewardPots(
//                epoch, adaPotsForPreviousEpoch.getReserves(), adaPotsForPreviousEpoch.getTreasury(), rewardProtocolParameters, epochInfo, rewardAddressesOfRetiredPoolsInEpoch, deregisteredAccounts,
//                mirCertificates, poolIds, poolStates, lateDeregisteredAccounts,
//                registeredAccountsSinceLastEpoch, registeredAccountsUntilNow, sharedPoolRewardAddressesWithoutReward,
//                deregisteredAccountsOnEpochBoundary);
//        long end = System.currentTimeMillis();
//        log.debug("Epoch calculation took " + Math.round((end - start) / 1000.0) + "s");
//
//        //update adapot treasury and reserves for epoch + 1
//
//        adaPotService.updateReserveAndTreasury(epoch, epochCalculationResult.getTreasury(), epochCalculationResult.getReserves());

        return rewardsCalcInput;
    }

    public EpochCalculationResult calculateEpochRewards(int epoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) {
            log.warn("Epoch " + epoch + " is before the start of the Shelley era. No rewards were calculated in this epoch.");
            return EpochCalculationResult.builder()
                    .totalRewardsPot(BigInteger.ZERO)
                    .treasury(BigInteger.ZERO)
                    .reserves(BigInteger.ZERO)
                    .treasuryCalculationResult(TreasuryCalculationResult.builder()
                            .totalRewardPot(BigInteger.ZERO)
                            .treasury(BigInteger.ZERO)
                            .treasuryWithdrawals(BigInteger.ZERO)
                            .unspendableEarnedRewards(BigInteger.ZERO)
                            .epoch(epoch).build())
                    .totalDistributedRewards(BigInteger.ZERO)
                    .epoch(epoch)
                    .build();
        } else if (epoch == MAINNET_SHELLEY_START_EPOCH) {
            return EpochCalculationResult.builder()
                    .totalRewardsPot(BigInteger.ZERO)
                    .treasury(BigInteger.ZERO)
                    .treasuryCalculationResult(TreasuryCalculationResult.builder()
                            .totalRewardPot(BigInteger.ZERO)
                            .treasury(BigInteger.ZERO)
                            .treasuryWithdrawals(BigInteger.ZERO)
                            .unspendableEarnedRewards(BigInteger.ZERO)
                            .epoch(epoch).build())
                    .reserves(MAINNET_SHELLEY_INITIAL_RESERVES)
                    .totalDistributedRewards(BigInteger.ZERO)
                    .epoch(epoch)
                    .build();
        }

        var rewardCalcInputs = fetchRewardCalcInputs(epoch);

        if(rewardCalcInputs == null) {
            log.error("Reward calculation inputs are null for epoch " + epoch);
            return null;
        }

        long start = System.currentTimeMillis();
        var epochCalculationResult = EpochCalculation.calculateEpochRewardPots(
                epoch,
                rewardCalcInputs.getReservesOfPreviousEpoch(),
                rewardCalcInputs.getTreasuryOfPreviousEpoch(),
                rewardCalcInputs.getProtocolParameters(),
                rewardCalcInputs.getEpochInfo(),
                rewardCalcInputs.getRewardAddressesOfRetiredPoolsInEpoch(),
                rewardCalcInputs.getDeregisteredAccounts(),
                rewardCalcInputs.getMirCertificates(),
                rewardCalcInputs.getPoolIds(),
                rewardCalcInputs.getPoolStates(),
                rewardCalcInputs.getLateDeregisteredAccounts(),
                rewardCalcInputs.getRegisteredAccountsSinceLastEpoch(),
                rewardCalcInputs.getRegisteredAccountsUntilNow(),
                rewardCalcInputs.getSharedPoolRewardAddressesWithoutReward(),
                rewardCalcInputs.getDeregisteredAccountsOnEpochBoundary());
        long end = System.currentTimeMillis();
        log.debug("Epoch calculation took " + Math.round((end - start) / 1000.0) + "s");

        return epochCalculationResult;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEpochRewards(int epoch, EpochCalculationResult epochCalculationResult) {
        if(epochCalculationResult != null) {
            adaPotService.updateReserveAndTreasury(epoch, epochCalculationResult.getTreasury(), epochCalculationResult.getReserves());

            //Update member rewards
            updateRewards(epoch, epochCalculationResult.getPoolRewardCalculationResults());
        } else
            log.error("Epoch calculation result is null for epoch " + epoch);
    }

    public void calculateAndUpdateEpochRewards(int epoch) {
        var epochCalculationResult = calculateEpochRewards(epoch);
        if(epochCalculationResult != null) {
            adaPotService.updateReserveAndTreasury(epoch, epochCalculationResult.getTreasury(), epochCalculationResult.getReserves());

            //Update member rewards
            updateRewards(epoch, epochCalculationResult.getPoolRewardCalculationResults());
        }
    }

    private void updateRewards(int epoch, List<PoolRewardCalculationResult> poolRewardCalculationResults) {
        if(poolRewardCalculationResults == null || poolRewardCalculationResults.isEmpty())
            return;

        var spendableEpochStartAbsoluteSlot = eraService.getShelleyAbsoluteSlot(epoch, 0);

        int earnedEpoch = epoch - 2;
        //Delete any existing member or leader rewards for the epoch. Re-run scenario
        rewardStorage.deleteLeaderMemberRewards(earnedEpoch);

        for(PoolRewardCalculationResult poolRewardCalculationResult: poolRewardCalculationResults) {
            String poolRewardAccount = poolRewardCalculationResult.getRewardAddress();
            BigInteger leaderRewardAmt = poolRewardCalculationResult.getOperatorReward();

            List<com.bloxbean.cardano.yaci.store.adapot.domain.Reward> poolRewards = new ArrayList<>();
            String poolHash = PoolUtil.getPoolHash(poolRewardCalculationResult.getPoolId());

            var leaderReward = com.bloxbean.cardano.yaci.store.adapot.domain.Reward.builder()
                    .address(poolRewardAccount)
                    .amount(leaderRewardAmt)
                    .poolId(poolHash)
                    .earnedEpoch(earnedEpoch)
                    .spendableEpoch(epoch)
                    .type(RewardType.leader)
                    .slot(spendableEpochStartAbsoluteSlot)
                    .build();

            poolRewards.add(leaderReward);

            Set<Reward> memberRewards = poolRewardCalculationResult.getMemberRewards();

            if(memberRewards != null && !memberRewards.isEmpty()) {

                var memberRewardsList = memberRewards.stream().map(reward -> {
                   return com.bloxbean.cardano.yaci.store.adapot.domain.Reward.builder()
                           .address(reward.getStakeAddress())
                           .amount(reward.getAmount())
                           .poolId(poolHash)
                           .earnedEpoch(earnedEpoch)
                           .spendableEpoch(epoch)
                           .type(RewardType.member)
                           .slot(spendableEpochStartAbsoluteSlot)
                           .build();
               }).toList();

               poolRewards.addAll(memberRewardsList);
            }

            rewardStorage.saveRewards(poolRewards);
            log.info("Rewards updated for pool : " + poolHash + " for epoch " + epoch + " with rewards : " + poolRewards.size());
        }
    }
}
