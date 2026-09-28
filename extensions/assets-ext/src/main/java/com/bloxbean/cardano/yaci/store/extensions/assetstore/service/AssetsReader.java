package com.bloxbean.cardano.yaci.store.extensions.assetstore.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService.BatchPrefetchData;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Top-level service for querying token metadata across CIP standards.
 * <p>
 * This is the primary API for consumers using the assets extension
 * as a library dependency (via the starter) without REST controllers.
 *
 * <ul>
 *   <li>{@link #getSubject} — merged metadata with configurable CIP priority</li>
 *   <li>{@link #getCip26Metadata} — CIP-26 offchain metadata only</li>
 *   <li>{@link #getCip68Metadata} — CIP-68 on-chain reference NFT metadata only</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetsReader {

    private final TokenQueryService tokenQueryService;
    private final Cip26StorageReader cip26StorageReader;
    private final Cip68StorageReader cip68StorageReader;

    // ========== Merged queries ==========

    /**
     * Query merged metadata for a subject using default priority (CIP_68, CIP_26).
     *
     * @param subject the subject (policyId + hex assetName)
     * @return the merged subject with metadata, or empty if not found
     */
    public Optional<Subject> getSubject(String subject) {
        return getSubject(subject, List.of(QueryPriority.CIP_68, QueryPriority.CIP_26));
    }

    /**
     * Query merged metadata for a subject with explicit priority.
     *
     * @param subject       the subject (policyId + hex assetName)
     * @param queryPriority ordered list of CIP standards to query
     * @return the merged subject with metadata, or empty if not found
     */
    public Optional<Subject> getSubject(String subject, List<QueryPriority> queryPriority) {
        return getSubject(subject, queryPriority, List.of());
    }

    /**
     * Query merged metadata for a subject with priority and property filtering.
     *
     * @param subject       the subject (policyId + hex assetName)
     * @param queryPriority ordered list of CIP standards to query
     * @param properties    list of property names to include (empty = all)
     * @return the merged subject with metadata, or empty if not found
     */
    public Optional<Subject> getSubject(String subject, List<QueryPriority> queryPriority, List<String> properties) {
        return tokenQueryService.querySubject(subject, queryPriority, properties, false);
    }

    /**
     * Batch query merged metadata for multiple subjects.
     *
     * @param subjects      list of subject identifiers
     * @param queryPriority ordered list of CIP standards
     * @return list of subjects with valid metadata (invalid/not-found subjects are excluded)
     */
    public List<Subject> getSubjects(List<String> subjects, List<QueryPriority> queryPriority) {
        BatchPrefetchData prefetchData = tokenQueryService.prefetchBatch(subjects, List.of());
        return subjects.stream()
                .map(subject -> tokenQueryService.querySubjectBatch(
                        subject, queryPriority, List.of(), prefetchData, false))
                .filter(subject -> subject.metadata() != null && subject.metadata().isValid())
                .toList();
    }

    // ========== Per-CIP queries (via StorageReader interfaces) ==========

    /**
     * Look up CIP-26 offchain metadata for a subject.
     */
    public Optional<Cip26Metadata> getCip26Metadata(String subject) {
        return cip26StorageReader.findBySubject(subject);
    }

    /**
     * Look up CIP-26 logo for a subject.
     */
    public Optional<String> getCip26Logo(String subject) {
        return cip26StorageReader.findLogoBySubject(subject);
    }

    /**
     * Look up CIP-68 on-chain reference NFT metadata for a subject.
     * Handles the fungible token prefix to reference NFT prefix conversion.
     */
    public Optional<FungibleTokenMetadata> getCip68Metadata(String subject) {
        return cip68StorageReader.findBySubject(subject);
    }

}
