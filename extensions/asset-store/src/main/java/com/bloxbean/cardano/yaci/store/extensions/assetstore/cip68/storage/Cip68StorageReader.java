package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to CIP-68 on-chain reference NFT metadata.
 */
public interface Cip68StorageReader {

    /**
     * Find the latest CIP-68 metadata for a reference NFT.
     *
     * @param policyId  the policy ID
     * @param assetName the reference NFT asset name (with 000643b0 prefix)
     * @return the parsed metadata if found
     */
    Optional<FungibleTokenMetadata> findByPolicyIdAndAssetName(String policyId, String assetName);

    /**
     * Find the latest CIP-68 metadata for a subject (auto-converts
     * fungible token prefix 0014df10 to reference NFT prefix 000643b0).
     *
     * @param subject the subject (policyId + hex assetName)
     * @return the parsed metadata if found
     */
    Optional<FungibleTokenMetadata> findBySubject(String subject);
}
