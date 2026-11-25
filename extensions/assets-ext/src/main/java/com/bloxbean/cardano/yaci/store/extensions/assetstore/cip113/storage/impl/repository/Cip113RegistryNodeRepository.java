package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNodeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The versions of the CIP-113 registry nodes. The queries are JPQL, so that {@code key} and {@code next}, reserved
 * words in H2 and MySQL, need no dialect-specific quoting.
 */
@Repository
public interface Cip113RegistryNodeRepository extends JpaRepository<Cip113RegistryNode, Cip113RegistryNodeId> {

    /** The current node of a key: the latest by slot, then by transaction index within the slot. */
    Optional<Cip113RegistryNode> findFirstByKeyOrderBySlotDescTxIndexDesc(String key);

    /**
     * The current node of each of the keys, one row per key: the highest slot, then the highest {@code tx_index} within
     * that slot. A node is in at most one output per transaction (it holds the one NFT of its key), so the pair is unique.
     */
    @Query("SELECT e FROM Cip113RegistryNode e " +
            "WHERE e.key IN :keys " +
            "AND e.slot = (SELECT MAX(e2.slot) FROM Cip113RegistryNode e2 WHERE e2.key = e.key) " +
            "AND e.txIndex = (SELECT MAX(e3.txIndex) FROM Cip113RegistryNode e3 WHERE e3.key = e.key AND e3.slot = e.slot)")
    List<Cip113RegistryNode> findLatestByKeys(@Param("keys") Collection<String> keys);

    /** Bulk delete on rollback: one SQL statement instead of loading and deleting each row. */
    @Modifying
    @Query("DELETE FROM Cip113RegistryNode e WHERE e.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);

}
