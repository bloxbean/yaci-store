package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNftId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataReferenceNftRepository extends JpaRepository<MetadataReferenceNft, MetadataReferenceNftId> {

    // --- Label-aware queries (use these for FT/NFT-specific lookups) ---

    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameAndLabelOrderBySlotDesc(
            String policyId, String assetName, int label);

    List<MetadataReferenceNft> findByPolicyIdAndLabel(String policyId, int label);

    Slice<MetadataReferenceNft> findByPolicyIdAndAssetNameAndLabelOrderBySlotDesc(
            String policyId, String assetName, int label, Pageable pageable);

    /**
     * Find the latest entry per (policyId, assetName) for multiple policies, filtered by label.
     * Uses a non-correlated subquery with GROUP BY for efficient execution across all DB engines.
     */
    @Query("SELECT e FROM MetadataReferenceNft e WHERE e.label = :label AND e.policyId IN :policyIds " +
            "AND CONCAT(e.policyId, ':', e.assetName, ':', e.slot) IN " +
            "(SELECT CONCAT(e2.policyId, ':', e2.assetName, ':', MAX(e2.slot)) " +
            "FROM MetadataReferenceNft e2 WHERE e2.label = :label AND e2.policyId IN :policyIds " +
            "GROUP BY e2.policyId, e2.assetName)")
    List<MetadataReferenceNft> findLatestByPolicyIdsAndLabel(@Param("policyIds") Collection<String> policyIds, @Param("label") int label);

    long countByLabelAndPolicyIdNotNull(int label);

    // --- Label-agnostic queries (for rollback and cross-label operations) ---

    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameOrderBySlotDesc(String policyId, String assetName);

    List<MetadataReferenceNft> findByPolicyId(String policyId);

    /**
     * Find the latest entry per (policyId, assetName) for multiple policies (all labels).
     * Uses a non-correlated subquery with GROUP BY.
     */
    @Query("SELECT e FROM MetadataReferenceNft e WHERE e.policyId IN :policyIds " +
            "AND CONCAT(e.policyId, ':', e.assetName, ':', e.slot) IN " +
            "(SELECT CONCAT(e2.policyId, ':', e2.assetName, ':', MAX(e2.slot)) " +
            "FROM MetadataReferenceNft e2 WHERE e2.policyId IN :policyIds " +
            "GROUP BY e2.policyId, e2.assetName)")
    List<MetadataReferenceNft> findLatestByPolicyIds(@Param("policyIds") Collection<String> policyIds);

    int deleteBySlotGreaterThan(Long slot);
}
