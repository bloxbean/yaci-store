package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read-only access to CIP-68 on-chain reference NFT metadata.
 * <p>
 * CIP-68 metadata is parsed from inline datums of reference NFTs (asset name prefix {@code 000643b0}).
 * The corresponding fungible token uses prefix {@code 0014df10}.
 */
public interface Cip68StorageReader {

    /**
     * Find the latest CIP-68 metadata for a reference NFT by policy ID and asset name.
     *
     * @param policyId  the policy ID
     * @param assetName the reference NFT asset name (with {@code 000643b0} prefix)
     * @return the parsed metadata if found
     */
    Optional<FungibleTokenMetadata> findByPolicyIdAndAssetName(String policyId, String assetName);

    /**
     * Find the latest CIP-68 metadata for a subject.
     * <p>
     * Automatically converts the fungible token prefix ({@code 0014df10}) to the
     * reference NFT prefix ({@code 000643b0}) before querying.
     *
     * @param subject the subject (policyId + hex assetName)
     * @return the parsed metadata if found
     */
    Optional<FungibleTokenMetadata> findBySubject(String subject);

    /**
     * Find the latest CIP-68 metadata for a subject with property filtering.
     *
     * @param subject    the subject (policyId + hex assetName)
     * @param properties list of property names to include (empty = all)
     * @return the parsed metadata if found, with only requested properties populated
     */
    Optional<FungibleTokenMetadata> findBySubject(String subject, List<String> properties);

    /**
     * Find all reference NFTs registered under a policy ID.
     * Returns the raw entity (not parsed metadata) including slot and datum.
     *
     * @param policyId the policy ID
     * @return all reference NFT entries for this policy
     */
    List<MetadataReferenceNft> findAllByPolicyId(String policyId);

    /**
     * Get the update history for a specific reference NFT (ordered by slot descending).
     * Each entry represents a datum change observed on-chain.
     *
     * @param policyId  the policy ID
     * @param assetName the reference NFT asset name (with {@code 000643b0} prefix)
     * @param page      zero-based page number
     * @param count     results per page
     * @return history of datum updates, newest first
     */
    List<MetadataReferenceNft> findHistory(String policyId, String assetName, int page, int count);

    /**
     * Find the latest reference NFT per (policyId, assetName) for multiple policies (batch).
     * Returns one entry per distinct token, using the highest slot.
     *
     * @param policyIds the policy IDs to look up
     * @return latest reference NFT entities for each token under the given policies
     */
    List<MetadataReferenceNft> findLatestByPolicyIds(Collection<String> policyIds);

    /**
     * Get the total number of indexed CIP-68 reference NFTs.
     *
     * @return the count
     */
    long count();
}
