package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface DRepDistStorageReader {
    Optional<BigInteger> getTotalStakeForEpoch(Integer epoch);
    Optional<BigInteger> getTotalStakeExcludeInactiveDRepForEpoch(Integer epoch);
    List<DRepDist> getAllByEpochAndDRepIds(Integer epoch, List<String> dRepIds);
    List<DRepDist> getAllByEpochAndDRepIdsExcludeInactiveDReps(Integer epoch, List<String> dRepIds);

    Optional<BigInteger> getStakeByDRepAndEpoch(String drepId, Integer epoch);
    Optional<BigInteger> getStakeByDRepTypeAndEpoch(DrepType drepType, Integer epoch);
}