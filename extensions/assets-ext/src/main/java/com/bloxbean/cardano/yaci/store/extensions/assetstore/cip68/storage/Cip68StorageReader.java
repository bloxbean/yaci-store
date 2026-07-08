package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;

import java.util.List;
import java.util.Map;
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

    /**
     * Batch lookup of CIP-68 fungible token metadata for multiple subjects.
     * Performs the fungible-to-reference-NFT prefix conversion internally
     * ({@code 0014df10} → {@code 000643b0}) and issues a single DB query.
     * <p>
     * The returned map is keyed by the <b>original fungible token subject</b>
     * (the string passed in by the caller), not the reference NFT subject —
     * the prefix conversion is an internal detail. Subjects that do not match
     * a CIP-68 fungible token prefix are silently skipped.
     *
     * @param subjects   fungible-token subjects (policyId + {@code 0014df10} + hex name)
     * @param properties property filter (empty list = include all properties)
     * @return map keyed by the original fungible subject; subjects with no metadata are omitted
     */
    Map<String, FungibleTokenMetadata> findBySubjects(List<String> subjects, List<String> properties);
}
