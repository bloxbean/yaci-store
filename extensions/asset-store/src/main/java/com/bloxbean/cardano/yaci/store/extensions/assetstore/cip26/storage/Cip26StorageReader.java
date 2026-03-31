package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read-only access to CIP-26 offchain token metadata.
 * <p>
 * CIP-26 metadata is synced from the GitHub cardano-token-registry.
 * The subject identifier is the concatenation of policy ID and hex-encoded asset name.
 */
public interface Cip26StorageReader {

    /**
     * Find metadata for a single subject.
     *
     * @param subject the subject (policyId + hex assetName)
     * @return metadata if registered
     */
    Optional<TokenMetadata> findBySubject(String subject);

    /**
     * Batch lookup metadata for multiple subjects.
     *
     * @param subjects list of subject identifiers
     * @return list of found metadata (missing subjects are omitted)
     */
    List<TokenMetadata> findBySubjects(List<String> subjects);

    /**
     * Find the logo (base64-encoded PNG) for a subject.
     *
     * @param subject the subject identifier
     * @return the logo string if registered
     */
    Optional<String> findLogoBySubject(String subject);

    /**
     * Find all tokens registered under a policy ID.
     *
     * @param policyId the policy ID (56 hex chars)
     * @return all metadata entries for this policy
     */
    List<TokenMetadata> findByPolicy(String policyId);

    /**
     * Search tokens by name (case-insensitive substring match).
     *
     * @param name  the search term
     * @param page  zero-based page number
     * @param count results per page (max 100)
     * @return matching metadata entries
     */
    List<TokenMetadata> searchByName(String name, int page, int count);

    /**
     * Find tokens by exact ticker (case-insensitive).
     *
     * @param ticker the ticker symbol (e.g. "ADA", "HOSKY")
     * @param page   zero-based page number
     * @param count  results per page
     * @return matching metadata entries
     */
    List<TokenMetadata> findByTicker(String ticker, int page, int count);

    /**
     * Find all tokens registered under multiple policy IDs (batch).
     *
     * @param policyIds the policy IDs to look up
     * @return all metadata entries matching any of the given policies
     */
    List<TokenMetadata> findByPolicies(Collection<String> policyIds);

    /**
     * Get the total number of registered CIP-26 tokens.
     *
     * @return the count
     */
    long count();
}
