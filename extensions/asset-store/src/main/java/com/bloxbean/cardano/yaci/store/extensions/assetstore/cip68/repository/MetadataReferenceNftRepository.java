package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity.MetadataReferenceNftId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataReferenceNftRepository extends JpaRepository<MetadataReferenceNft, MetadataReferenceNftId> {

    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameOrderBySlotDesc(String policyId, String assetName);

    List<MetadataReferenceNft> findByPolicyId(String policyId);

    Slice<MetadataReferenceNft> findByPolicyIdAndAssetNameOrderBySlotDesc(String policyId, String assetName, Pageable pageable);

    long countByPolicyIdNotNull();

    int deleteBySlotGreaterThan(Long slot);
}
