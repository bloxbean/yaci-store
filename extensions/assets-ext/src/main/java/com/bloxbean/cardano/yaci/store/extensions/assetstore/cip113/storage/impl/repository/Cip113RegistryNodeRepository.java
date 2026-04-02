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

@Repository
public interface Cip113RegistryNodeRepository extends JpaRepository<Cip113RegistryNode, Cip113RegistryNodeId> {

    /**
     * Returns the most recent registry node state (highest slot) for a given policy ID.
     */
    Optional<Cip113RegistryNode> findFirstByPolicyIdOrderBySlotDesc(String policyId);

    /**
     * Returns the latest registry node state per policy ID.
     * Uses ROW_NUMBER() window function — portable across PostgreSQL, H2, and MySQL 8+.
     */
    @Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY policy_id ORDER BY slot DESC) AS rn " +
            "FROM cip113_registry_node WHERE policy_id IN (:policyIds)) ranked WHERE rn = 1",
            nativeQuery = true)
    List<Cip113RegistryNode> findLatestByPolicyIds(@Param("policyIds") Collection<String> policyIds);

    @Query("SELECT DISTINCT e.policyId FROM Cip113RegistryNode e")
    List<String> findDistinctPolicyIds();

    int deleteBySlotGreaterThan(Long slot);

}
