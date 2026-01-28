package com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.EpochEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BFEpochRepository extends JpaRepository<EpochEntity, Long> {

    @Query("select e from EpochEntity e where e.number > :epoch order by e.number asc")
    List<EpochEntity> findNextEpochs(@Param("epoch") long epoch, Pageable pageable);

    @Query("select e from EpochEntity e where e.number < :epoch order by e.number desc")
    List<EpochEntity> findPreviousEpochs(@Param("epoch") long epoch, Pageable pageable);
}
