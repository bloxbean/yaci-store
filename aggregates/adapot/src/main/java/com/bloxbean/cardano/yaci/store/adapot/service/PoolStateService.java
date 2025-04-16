package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.Delegator;
import org.cardanofoundation.rewards.calculation.domain.PoolBlock;
import org.cardanofoundation.rewards.calculation.domain.PoolState;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for retrieving historical pool state information within a given epoch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PoolStateService {
    private final StoreProperties storeProperties;
    private final PoolStorageReader poolStorageReader;
    private final EpochStakeStorageReader epochStakeStorage;

    public List<PoolState> getHistoryOfAllPoolsInEpoch(Integer epoch, List<PoolBlock> poolBlocksInEpoch) {
        List<PoolState> poolHistories = new ArrayList<>();

        if (poolBlocksInEpoch.isEmpty())
            return poolHistories;

        int batchSize = 20;

        List<String> poolIds = poolBlocksInEpoch.stream()
                .map(PoolBlock::getPoolId)
                .distinct()
                .toList();

        List<PoolDetails> latestPoolDetails = poolStorageReader.getPoolDetails(poolIds, epoch); //TODO -- check if only update and registration status are required

        List<List<String>> poolIdBatches = ListUtil.partition(poolIds, batchSize);

        int i = 0;
        int batches = poolIdBatches.size();
        for (List<String> poolIdBatch : poolIdBatches) {
            log.info("fetching pool history batch " + i + " / " + batches + " for epoch " + epoch + " with " + poolIdBatch.size() + " pools");
            List<EpochStake> delgatorStakes = epochStakeStorage.getAllActiveStakesByEpochAndPools(epoch, poolIdBatch);

            for (String poolId : poolIdBatch) {
                PoolState poolState = new PoolState();

                HashSet<Delegator> delegators = delgatorStakes.stream()
                        .filter(epochStake -> epochStake.getPoolId().equals(poolId))
                        .filter(epochStake -> epochStake.getAmount() != null
                                && epochStake.getAmount().compareTo(BigInteger.ZERO) == 1)
                        .map(epochStake -> Delegator.builder()
                                .stakeAddress(epochStake.getAddress())
                                .activeStake(epochStake.getAmount())
                                .build())
                        .collect(Collectors.toCollection(HashSet::new));

                BigInteger activeStake = delegators.stream()
                        .map(Delegator::getActiveStake)
                        .reduce(BigInteger::add)
                        .orElse(BigInteger.ZERO);

                poolState.setActiveStake(activeStake);
                poolState.setDelegators(delegators);

                Integer blockCount = poolBlocksInEpoch.stream()
                        .filter(poolBlocks -> poolBlocks.getPoolId().equals(poolId))
                        .map(PoolBlock::getBlockCount)
                        .findFirst()
                        .orElse(0);
                poolState.setBlockCount(blockCount);

                PoolDetails latestUpdate = latestPoolDetails.stream()
                        .filter(update -> update.getPoolId().equals(poolId))
                        .findFirst()
                        .orElse(null);

                if (latestUpdate == null) {
                    log.info("No update for pool " + poolId + " in epoch " + epoch);
                    continue;
                }

                String rewardAccount = latestUpdate.getRewardAccount();
                if (storeProperties.isMainnet() && rewardAccount.startsWith("stake_test")) { //testnet addr in mainnet
                    //convert it to mainnet address
                    var stakeAddr = new Address(rewardAccount);
                    var stakeCred = stakeAddr.getDelegationCredential().orElse(null);
                    if (stakeCred != null) {
                        rewardAccount = AddressProvider.getRewardAddress(stakeCred, Networks.mainnet()).toBech32();
                    }
                } else if (!storeProperties.isMainnet() && !rewardAccount.startsWith("stake_test")) { //mainnet address in testnet
                    //convert it to testnet address
                    var stakeAddr = new Address(rewardAccount);
                    var stakeCred = stakeAddr.getDelegationCredential().orElse(null);
                    if (stakeCred != null) {
                        rewardAccount = AddressProvider.getRewardAddress(stakeCred, Networks.testnet()).toBech32();
                    }
                }

                poolState.setRewardAddress(rewardAccount);

                poolState.setFixedCost(latestUpdate.getCost());
                poolState.setMargin(latestUpdate.getMargin() != null? latestUpdate.getMargin().doubleValue() : 0);
                poolState.setPledge(latestUpdate.getPledge());
                poolState.setEpoch(epoch);
                poolState.setPoolId(PoolUtil.getBech32PoolId(poolId)); //bech32

                var poolOwnerStakeAddresses = latestUpdate.getPoolOwners()
                        .stream()
                        .map(addressHex ->
                                AddressProvider.getRewardAddress(Credential.fromKey(HexUtil.decodeHexString(addressHex)),
                                        storeProperties.isMainnet() ? Networks.mainnet() : Networks.testnet()).toBech32())
                        .collect(Collectors.toSet());


                poolState.setOwners(new HashSet<>(poolOwnerStakeAddresses));

                BigInteger poolOwnerActiveStake = BigInteger.ZERO;
                for (Delegator delegator : delegators) {
                    Address delegatorStakeAddress = new Address(delegator.getStakeAddress());
                    var delegatorStakeAddressHex = delegatorStakeAddress.getDelegationCredentialHash()
                            .map(bytes -> HexUtil.encodeHexString(bytes))
                            .orElse(null);

                    if (latestUpdate.getPoolOwners() != null && latestUpdate.getPoolOwners().contains(delegatorStakeAddressHex)) {
                        poolOwnerActiveStake = poolOwnerActiveStake.add(delegator.getActiveStake());
                    }
                }

                poolState.setOwnerActiveStake(poolOwnerActiveStake);
                poolHistories.add(poolState);
            }
        }

        return poolHistories;
    }

}
