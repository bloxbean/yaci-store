package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.OffChainSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncStateRepository extends JpaRepository<OffChainSyncState, Long> {
    Optional<OffChainSyncState> findTopByOrderByIdDesc();
}
