package com.bloxbean.cardano.yaci.store.extensions.assetstore.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.MetadataV2QueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Top-level service for querying token metadata across CIP standards.
 * <p>
 * This is the primary API for consumers using the asset-store extension
 * as a library dependency (via the starter) without REST controllers.
 *
 * <ul>
 *   <li>{@link #getSubject} — merged metadata with configurable CIP priority</li>
 *   <li>{@link #getCip26Metadata} — CIP-26 offchain metadata only</li>
 *   <li>{@link #getCip68Metadata} — CIP-68 on-chain reference NFT metadata only</li>
 *   <li>{@link #getCip113RegistryNode} — CIP-113 programmable token info only</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetStoreReader {

    private final MetadataV2QueryService metadataV2QueryService;
    private final Cip26StorageReader cip26StorageReader;
    private final Cip68StorageReader cip68StorageReader;
    private final Cip113StorageReader cip113StorageReader;

    // ========== Merged (V2) queries ==========

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
        Subject result = metadataV2QueryService.querySubject(subject, queryPriority, properties, false);
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
        Map<String, ProgrammableTokenCip113> cip113Map = metadataV2QueryService.prefetchCip113(subjects);
        return subjects.stream()
                .map(subject -> metadataV2QueryService.querySubjectBatch(
                        subject, queryPriority, List.of(), cip113Map, false))
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
    public Optional<Cip68TokenMetadata> getCip68Metadata(String subject) {
        return cip68StorageReader.findBySubject(subject);
    }

    /**
     * Look up CIP-113 programmable token registry node for a policy ID.
     */
    public Optional<ProgrammableTokenCip113> getCip113RegistryNode(String policyId) {
        return cip113StorageReader.findByPolicyId(policyId);
    }

    /**
     * Batch look up CIP-113 registry nodes for multiple policy IDs.
     */
    public Map<String, ProgrammableTokenCip113> getCip113RegistryNodes(Collection<String> policyIds) {
        return cip113StorageReader.findByPolicyIds(policyIds);
    }

    /**
     * Check whether a policy ID is registered as a CIP-113 programmable token.
     */
    public boolean isProgrammableToken(String policyId) {
        return cip113StorageReader.isProgrammableToken(policyId);
    }
}
