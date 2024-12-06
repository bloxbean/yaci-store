package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface DRepDistStorageReader {
    Optional<BigInteger> getTotalStakeForEpoch(Integer epoch);
    List<DRepDist> getAllByEpochAndDReps(Integer epoch, List<String> dRepHashList);
    Optional<BigInteger> getStakeByDRepAndEpoch(String dRepHash, Integer epoch);
}