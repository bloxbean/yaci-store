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
     * Returns the latest registry node state per policy ID.
     * Uses a correlated subquery to find the max slot per policy, portable across databases.
     */
    @Query("SELECT e FROM Cip113RegistryNode e WHERE e.policyId IN :policyIds AND e.slot = " +
            "(SELECT MAX(e2.slot) FROM Cip113RegistryNode e2 WHERE e2.policyId = e.policyId)")
    List<Cip113RegistryNode> findLatestByPolicyIds(@Param("policyIds") Collection<String> policyIds);

    @Query("SELECT DISTINCT e.policyId FROM Cip113RegistryNode e")
    List<String> findDistinctPolicyIds();

    int deleteBySlotGreaterThan(Long slot);

}
