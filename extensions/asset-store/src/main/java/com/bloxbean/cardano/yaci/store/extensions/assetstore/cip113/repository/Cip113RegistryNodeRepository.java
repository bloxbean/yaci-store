package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.entity.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.entity.Cip113RegistryNodeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface Cip113RegistryNodeRepository extends JpaRepository<Cip113RegistryNode, Cip113RegistryNodeId> {

    /**
     * Returns the most recent registry node state (highest slot) for a given policy ID.
     */
    Optional<Cip113RegistryNode> findFirstByPolicyIdOrderBySlotDesc(String policyId);

    /**
     * Returns the latest registry node state per policy ID using PostgreSQL DISTINCT ON.
     * Only returns one row per policy ID (the one with the highest slot).
     */
    @Query(value = "SELECT DISTINCT ON (policy_id) * FROM cip113_registry_node " +
            "WHERE policy_id IN :policyIds ORDER BY policy_id, slot DESC",
            nativeQuery = true)
    List<Cip113RegistryNode> findLatestByPolicyIds(@Param("policyIds") Collection<String> policyIds);

    int deleteBySlotGreaterThan(Long slot);

}
