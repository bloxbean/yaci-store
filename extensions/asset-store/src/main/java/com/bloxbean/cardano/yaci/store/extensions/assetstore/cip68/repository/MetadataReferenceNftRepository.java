package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity.MetadataReferenceNftId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetadataReferenceNftRepository extends JpaRepository<MetadataReferenceNft, MetadataReferenceNftId> {

    /**
     * Returns the most recent metadata (highest slot) for a given policyId and assetName.
     * Uses Spring Data JPA method naming convention to order by slot descending and return first result.
     */
    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameOrderBySlotDesc(String policyId, String assetName);

    int deleteBySlotGreaterThan(Long slot);

}
