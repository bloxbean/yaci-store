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
 * Repository for the CIP-113 programmable-token registry node table.
 *
 * <p><b>On the word "key":</b> the {@code key} column of {@link Cip113RegistryNode} is the
 * registry node's sort key in the on-chain sorted linked list. For real registration rows it
 * equals the 56-hex policy ID of the registered programmable token; for the two sentinel rows
 * per registry (head and tail) it is a non-policy-ID linked-list marker. See
 * {@link Cip113RegistryNode} for the full explanation. Methods on this repository
 * name their parameter {@code key} to stay honest about that dual role, but in practice
 * callers always pass a real 56-hex policy ID — there is never a reason to look up a sentinel.
 *
 * <p>All queries are JPQL rather than native SQL so that the {@code key} and {@code next}
 * identifiers — reserved words in H2 and MySQL — do not need dialect-specific quoting.
 */
@Repository
public interface Cip113RegistryNodeRepository extends JpaRepository<Cip113RegistryNode, Cip113RegistryNodeId> {

    /**
     * Returns the most recent registry node state for a given key — highest slot, and within that
     * slot the highest tx_index (so intra-block updates resolve deterministically to the last one).
     * Callers typically pass the 56-hex policy ID of the token they're looking up.
     */
    Optional<Cip113RegistryNode> findFirstByKeyOrderBySlotDescTxIndexDesc(String key);

    /**
     * Returns the latest registry node state for each of the given keys — exactly one row per key.
     * "Latest" = highest slot, and within that slot the highest tx_index, so the (key, slot, tx_index)
     * history collapses to a single deterministic current-state row per key. The two correlated
     * subqueries pin the row to MAX(slot) then MAX(tx_index) within that slot; since
     * (key, slot, tx_index) is unique this yields one row per key and never a duplicate-key collision
     * downstream. Written as JPQL so the reserved-word {@code key} identifier needs no dialect quoting.
     *
     * <p>Index-backed per-key lookups on PostgreSQL / H2 / MySQL given the
     * {@code (key, slot, tx_index)} primary key.
     */
    @Query("SELECT e FROM Cip113RegistryNode e " +
            "WHERE e.key IN :keys " +
            "AND e.slot = (SELECT MAX(e2.slot) FROM Cip113RegistryNode e2 WHERE e2.key = e.key) " +
            "AND e.txIndex = (SELECT MAX(e3.txIndex) FROM Cip113RegistryNode e3 " +
            "WHERE e3.key = e.key AND e3.slot = e.slot)")
    List<Cip113RegistryNode> findLatestByKeys(@Param("keys") Collection<String> keys);

    /**
     * Bulk delete on rollback. {@code @Modifying @Query} with JPQL issues a single SQL DELETE;
     * without these annotations Spring Data's derived deleteBy* loads each entity then deletes
     * per-row (one round-trip per matching row), which is pathological for deep rollbacks.
     * Caller ({@code Cip113RollbackProcessor.handleRollbackEvent}) is already {@code @Transactional}.
     */
    @Modifying
    @Query("DELETE FROM Cip113RegistryNode e WHERE e.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);

}
