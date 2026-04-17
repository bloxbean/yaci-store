package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNodeId;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Returns the most recent registry node state (highest slot) for a given key.
     * Callers typically pass the 56-hex policy ID of the token they're looking up.
     */
    Optional<Cip113RegistryNode> findFirstByKeyOrderBySlotDesc(String key);

    /**
     * Returns the latest registry node state for each of the given keys — one row per key,
     * the row with the highest slot. Written as JPQL with a correlated subquery so that the
     * reserved-word {@code key} identifier does not need dialect-specific quoting in the SQL.
     *
     * <p>The correlated {@code MAX(slot)} subquery is efficient given the
     * {@code (key, slot)} portion of the primary key; PostgreSQL / H2 / MySQL all plan this
     * as an index-backed per-key lookup.
     */
    @Query("SELECT e FROM Cip113RegistryNode e " +
            "WHERE e.key IN :keys " +
            "AND e.slot = (SELECT MAX(e2.slot) FROM Cip113RegistryNode e2 WHERE e2.key = e.key)")
    List<Cip113RegistryNode> findLatestByKeys(@Param("keys") Collection<String> keys);

    int deleteBySlotGreaterThan(Long slot);

}
