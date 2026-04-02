package com.bloxbean.cardano.yaci.store.extensions.assetstore.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService.BatchPrefetchData;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
 *   <li>{@link #getCip113RegistryNode} — CIP-113 programmable token info only (when enabled)</li>
 * </ul>
 */
@Service
@Slf4j
public class AssetsReader {

    private final TokenQueryService tokenQueryService;
    private final Cip26StorageReader cip26StorageReader;
    private final Cip68StorageReader cip68StorageReader;
    private final Optional<Cip113StorageReader> cip113StorageReader;

    @Autowired
    public AssetsReader(TokenQueryService tokenQueryService,
                            Cip26StorageReader cip26StorageReader,
                            Cip68StorageReader cip68StorageReader,
                            @Autowired(required = false) Cip113StorageReader cip113StorageReader) {
        this.tokenQueryService = tokenQueryService;
        this.cip26StorageReader = cip26StorageReader;
        this.cip68StorageReader = cip68StorageReader;
        this.cip113StorageReader = Optional.ofNullable(cip113StorageReader);
    }

    // ========== Merged queries ==========

    /**
     * Query merged metadata for a subject using default priority (CIP_68, CIP_26).
     *
     * @param subject the subject (policyId + hex assetName)
     * @return the merged subject with metadata and extensions, or empty if not found
     */
    public Optional<Subject> getSubject(String subject) {
        return getSubject(subject, List.of(QueryPriority.CIP_68, QueryPriority.CIP_26));
    }

    /**
     * Query merged metadata for a subject with explicit priority.
     *
     * @param subject       the subject (policyId + hex assetName)
     * @param queryPriority ordered list of CIP standards to query
     * @return the merged subject with metadata and extensions, or empty if not found
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
     * @return the merged subject with metadata and extensions, or empty if not found
     */
    public Optional<Subject> getSubject(String subject, List<QueryPriority> queryPriority, List<String> properties) {
        Subject result = tokenQueryService.querySubject(subject, queryPriority, properties, false);
        return Optional.ofNullable(result);
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
    public Optional<TokenMetadata> getCip26Metadata(String subject) {
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

    /**
     * Look up CIP-113 programmable token registry node for a policy ID.
     * Returns empty if CIP-113 module is not enabled.
     */
    public Optional<ProgrammableTokenCip113> getCip113RegistryNode(String policyId) {
        return cip113StorageReader.flatMap(reader -> reader.findByPolicyId(policyId));
    }

    /**
     * Batch look up CIP-113 registry nodes for multiple policy IDs.
     * Returns empty map if CIP-113 module is not enabled.
     */
    public Map<String, ProgrammableTokenCip113> getCip113RegistryNodes(Collection<String> policyIds) {
        return cip113StorageReader.map(reader -> reader.findByPolicyIds(policyIds))
                .orElse(Map.of());
    }

    /**
     * Check whether a policy ID is registered as a CIP-113 programmable token.
     * Returns false if CIP-113 module is not enabled.
     */
    public boolean isProgrammableToken(String policyId) {
        return cip113StorageReader.map(reader -> reader.isProgrammableToken(policyId))
                .orElse(false);
    }
}
