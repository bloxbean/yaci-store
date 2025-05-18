package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovEpochActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface GovEpochActivityRepository extends JpaRepository<GovEpochActivityEntity, Integer>  {
    @Query("SELECT epoch FROM GovEpochActivityEntity WHERE dormant = true AND epoch >= :from AND epoch <= :to")
    Set<Integer> findDormantEpochsByEpochBetween(Integer from, Integer to);
}
