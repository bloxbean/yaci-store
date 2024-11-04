package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface EpochStakeStorageReader {

    Optional<BigInteger> getTotalActiveStakeByEpoch(Integer activeEpoch);

    Optional<EpochStake> getActiveStakeByAddressAndEpoch(String address, Integer activeEpoch);

    Optional<BigInteger> getActiveStakeByPoolAndEpoch(String poolId, Integer epoch);

    List<EpochStake> getAllActiveStakesByEpoch(Integer epoch, int page , int count);

    List<EpochStake> getAllActiveStakesByEpochAndPool(Integer epoch, String poolId, int page , int count);

    List<EpochStake> getAllActiveStakesByEpochAndPool(Integer epoch, String poolId);

    List<EpochStake> getAllActiveStakesByEpochAndPools(Integer epoch, List<String> poolIds);
}
