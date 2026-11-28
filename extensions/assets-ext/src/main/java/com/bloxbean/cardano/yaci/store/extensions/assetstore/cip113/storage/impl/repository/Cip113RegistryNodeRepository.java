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

    /**
     * The current node of a key in the given registries: the latest by slot, then by transaction index, then by output
     * index (two registries can each hold a node of the key in the same transaction).
     */
    Optional<Cip113RegistryNode> findFirstByKeyAndRegistryPolicyIdInOrderBySlotDescTxIndexDescOutputIndexDesc(
            String key, Collection<String> registryPolicyIds);

    /**
     * The latest versions of each of the keys in the given registries: the highest slot, then the highest
     * {@code tx_index} within that slot. Within one registry a node is in at most one output per transaction (it holds
     * the one NFT of its key), so a key has one row, unless two registries each hold a node of it in that transaction;
     * the caller then takes the highest {@code output_index}, as the single lookup does.
     */
    @Query("SELECT e FROM Cip113RegistryNode e " +
            "WHERE e.key IN :keys AND e.registryPolicyId IN :registries " +
            "AND e.slot = (SELECT MAX(e2.slot) FROM Cip113RegistryNode e2 " +
            "WHERE e2.key = e.key AND e2.registryPolicyId IN :registries) " +
            "AND e.txIndex = (SELECT MAX(e3.txIndex) FROM Cip113RegistryNode e3 " +
            "WHERE e3.key = e.key AND e3.slot = e.slot AND e3.registryPolicyId IN :registries)")
    List<Cip113RegistryNode> findLatestByKeys(@Param("keys") Collection<String> keys,
                                              @Param("registries") Collection<String> registryPolicyIds);

    /** Bulk delete on rollback: one SQL statement instead of loading and deleting each row. */
    @Modifying
    @Query("DELETE FROM Cip113RegistryNode e WHERE e.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);

}
