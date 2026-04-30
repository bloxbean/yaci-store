package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.OffChainSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncStateRepository extends JpaRepository<OffChainSyncState, Long> {
    Optional<OffChainSyncState> findTopByOrderByIdDesc();
}
