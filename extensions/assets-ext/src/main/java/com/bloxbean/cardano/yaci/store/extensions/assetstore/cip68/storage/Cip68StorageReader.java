package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read-only access to CIP-68 on-chain fungible token metadata.
 * <p>
 * All queries in this reader are scoped to <b>fungible tokens (label 333)</b>.
 * Future NFT support (label 222) will use a separate reader interface.
 */
public interface Cip68StorageReader {

    /**
     * Find the latest CIP-68 fungible token metadata by policy ID and asset name.
     */
    Optional<FungibleTokenMetadata> findByPolicyIdAndAssetName(String policyId, String assetName);

    /**
     * Find the latest CIP-68 fungible token metadata for a subject.
     * Automatically converts the fungible token prefix ({@code 0014df10}) to the
     * reference NFT prefix ({@code 000643b0}) before querying.
     */
    Optional<FungibleTokenMetadata> findBySubject(String subject);

    /**
     * Find the latest CIP-68 fungible token metadata for a subject with property filtering.
     */
    Optional<FungibleTokenMetadata> findBySubject(String subject, List<String> properties);

    /**
     * Find all fungible token reference NFTs registered under a policy ID.
     */
    List<MetadataReferenceNft> findAllByPolicyId(String policyId);

    /**
     * Get the update history for a specific reference NFT (ordered by slot descending).
     */
    List<MetadataReferenceNft> findHistory(String policyId, String assetName, int page, int count);

    /**
     * Find the latest fungible token reference NFT per (policyId, assetName) for multiple policies.
     */
    List<MetadataReferenceNft> findLatestByPolicyIds(Collection<String> policyIds);

    /**
     * Get the total number of indexed CIP-68 fungible token reference NFTs.
     */
    long count();
}
