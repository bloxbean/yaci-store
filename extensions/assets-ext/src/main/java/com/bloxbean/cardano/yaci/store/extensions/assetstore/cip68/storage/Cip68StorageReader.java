package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to CIP-68 on-chain fungible token metadata.
 * All queries are scoped to fungible tokens (label 333).
 */
public interface Cip68StorageReader {

    /**
     * Find the latest CIP-68 fungible token metadata by policy ID and asset name.
     */
    Optional<FungibleTokenMetadata> findByPolicyIdAndAssetName(String policyId, String assetName);

    /**
     * Find the latest CIP-68 fungible token metadata for a subject.
     * Automatically converts the fungible token prefix to reference NFT prefix.
     */
    Optional<FungibleTokenMetadata> findBySubject(String subject);

    /**
     * Find the latest CIP-68 fungible token metadata for a subject with property filtering.
     */
    Optional<FungibleTokenMetadata> findBySubject(String subject, List<String> properties);
}
