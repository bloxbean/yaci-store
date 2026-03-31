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

    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameOrderBySlotDesc(String policyId, String assetName);

    List<MetadataReferenceNft> findByPolicyId(String policyId);

    Slice<MetadataReferenceNft> findByPolicyIdAndAssetNameOrderBySlotDesc(String policyId, String assetName, Pageable pageable);

    @Query("SELECT e FROM MetadataReferenceNft e WHERE e.policyId IN :policyIds AND e.slot = " +
            "(SELECT MAX(e2.slot) FROM MetadataReferenceNft e2 WHERE e2.policyId = e.policyId AND e2.assetName = e.assetName)")
    List<MetadataReferenceNft> findLatestByPolicyIds(@Param("policyIds") Collection<String> policyIds);

    long countByPolicyIdNotNull();

    int deleteBySlotGreaterThan(Long slot);
}
