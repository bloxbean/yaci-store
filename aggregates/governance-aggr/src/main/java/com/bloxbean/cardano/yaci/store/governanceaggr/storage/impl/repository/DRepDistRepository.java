package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepDistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Repository
public interface DRepDistRepository extends JpaRepository<DRepDistEntity, String> {
    @Query("select sum(d.amount) from DRepDistEntity d where d.epoch = :epoch")
    Optional<BigInteger> getTotalStakeForEpoch(Integer epoch);

    @Query("select d from DRepDistEntity d where d.epoch = :epoch and d.drepId in :dRepIds")
    List<DRepDistEntity> getAllByEpochAndDRepIds(Integer epoch, List<String> dRepIds);

    @Query("select sum(d.amount) from DRepDistEntity d where d.epoch = :epoch and d.drepId= :dRepId")
    Optional<BigInteger> getStakeByDRepIdAndEpoch(Integer epoch, String dRepId);

    @Query("select sum(d.amount) from DRepDistEntity d where d.epoch = :epoch and d.drepType= :drepType")
    Optional<BigInteger> getStakeByDRepTypeAndEpoch(Integer epoch, DrepType drepType);
}
