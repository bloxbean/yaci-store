package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepExpiryEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepExpiryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DRepExpiryRepository extends JpaRepository<DRepExpiryEntity, DRepExpiryId> {
    List<DRepExpiryEntity> findByEpoch(Integer epoch);
}
