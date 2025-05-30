package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitteeStateRepository extends JpaRepository<CommitteeStateEntity, String> {
    @Query("SELECT c FROM CommitteeStateEntity c WHERE c.epoch = (SELECT MAX(c2.epoch) FROM CommitteeStateEntity c2)")
    Optional<CommitteeStateEntity> findByMaxEpoch();
}
