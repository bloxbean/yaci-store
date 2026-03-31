package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNftId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetadataReferenceNftRepository extends JpaRepository<MetadataReferenceNft, MetadataReferenceNftId> {

    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameAndLabelOrderBySlotDesc(
            String policyId, String assetName, int label);

    int deleteBySlotGreaterThan(Long slot);
}
