package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.*;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.Cip26TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core query and merge logic for multi-standard token metadata.
 * <p>
 * Merges CIP-26 and CIP-68 metadata based on a configurable priority order,
 * and appends CIP-113 extensions when applicable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenQueryService {

    private static final ResolvedMetadata IDENTITY = new ResolvedMetadata(Metadata.empty(), Standards.empty());

    private final Cip26StorageReader cip26StorageReader;
    private final Cip68StorageReader cip68StorageReader;
    private final Cip113StorageReader cip113StorageReader;

    /**
     * Query and merge metadata for a single subject.
     *
     * @param subject         the subject identifier (policyId + assetName hex)
     * @param queryPriority   ordered list of CIP standards to query
     * @param queryProperties list of properties to include (empty means all)
     * @param showCipsDetails whether to include raw per-standard metadata
     * @return the merged Subject, or empty if no valid metadata found
     */
    public Optional<Subject> querySubject(String subject,
                                          List<QueryPriority> queryPriority,
                                          List<String> queryProperties,
                                          boolean showCipsDetails) {

        ResolvedMetadata resolved = queryPriority.stream()
                .reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());

        if (resolved.metadata().isEmpty() || !resolved.metadata().isValid()) {
            return Optional.empty();
        }

        Map<String, Extension> extensions = buildExtensions(subject);
        TokenType type = extensions.isEmpty() ? TokenType.NATIVE : TokenType.PROGRAMMABLE;

        return Optional.of(new Subject(subject, type, resolved.metadata(),
                showCipsDetails ? resolved.standards() : null,
                extensions.isEmpty() ? null : extensions));
    }

    /**
     * Query and merge metadata for a single subject in batch context,
     * using pre-fetched data maps to avoid N+1 queries.
     */
    public Subject querySubjectBatch(String subject,
                                     List<QueryPriority> queryPriority,
                                     List<String> queryProperties,
                                     BatchPrefetchData prefetchData,
                                     boolean showCipsDetails) {

        ResolvedMetadata resolved = queryPriority.stream()
                .reduce(IDENTITY, combineStandardsBatch(subject, queryProperties, prefetchData), aggregateResults());

        Map<String, Extension> extensions = new LinkedHashMap<>();
        ProgrammableTokenCip113 cip113 = prefetchData.cip113Map().get(AssetType.fromUnit(subject).policyId());
        if (cip113 != null) {
            extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113);
        }

        TokenType type = extensions.isEmpty() ? TokenType.NATIVE : TokenType.PROGRAMMABLE;

        return new Subject(subject, type, resolved.metadata(),
                showCipsDetails ? resolved.standards() : null,
                extensions.isEmpty() ? null : extensions);
    }

    /**
     * Pre-fetch all data for a batch of subjects in bulk queries to avoid N+1.
     * Issues at most 4 queries regardless of batch size:
     * CIP-26 metadata, CIP-26 logos, CIP-68 reference NFTs, CIP-113 registry nodes.
     */
    public BatchPrefetchData prefetchBatch(List<String> subjects, List<String> queryProperties) {
        // CIP-113: one query for all distinct policy IDs
        List<String> policyIds = subjects.stream()
                .map(s -> AssetType.fromUnit(s).policyId())
                .distinct()
                .toList();
        Map<String, ProgrammableTokenCip113> cip113Map = cip113StorageReader.findByPolicyIds(policyIds);

        // CIP-26: one query for all metadata, one query for all logos
        Map<String, TokenMetadata> cip26MetadataMap = cip26StorageReader.findBySubjects(subjects).stream()
                .collect(Collectors.toMap(TokenMetadata::getSubject, Function.identity()));

        boolean needLogos = queryProperties.isEmpty() || queryProperties.contains("logo");
        Map<String, String> cip26LogoMap = needLogos
                ? cip26StorageReader.findLogosBySubjects(subjects)
                : Map.of();

        // CIP-68: one query for all fungible token subjects, result keyed by the original subject.
        // Non-fungible subjects are silently filtered inside findBySubjects, so passing the full
        // batch is safe and avoids a double prefix-check here.
        Map<String, FungibleTokenMetadata> cip68MetadataMap = cip68StorageReader.findBySubjects(subjects, queryProperties);

        return new BatchPrefetchData(cip113Map, cip26MetadataMap, cip26LogoMap, cip68MetadataMap);
    }

    /**
     * Pre-fetched data for batch operations. Eliminates N+1 queries.
     */
    public record BatchPrefetchData(
            Map<String, ProgrammableTokenCip113> cip113Map,
            Map<String, TokenMetadata> cip26MetadataMap,
            Map<String, String> cip26LogoMap,
            Map<String, FungibleTokenMetadata> cip68MetadataMap) {
    }

    private Optional<ResolvedMetadata> findMetadata(String subject, List<String> properties, QueryPriority priority) {
        return switch (priority) {
            case CIP_26 -> findCip26Metadata(subject, properties);
            case CIP_68 -> findCip68Metadata(subject, properties);
        };
    }

    private Optional<ResolvedMetadata> findCip26Metadata(String subject, List<String> properties) {
        return cip26StorageReader.findBySubject(subject)
                .map(entity -> {
                    // Only fetch logo if all properties requested or logo explicitly requested
                    String logo = (properties.isEmpty() || properties.contains("logo"))
                            ? cip26StorageReader.findLogoBySubject(subject).orElse(null)
                            : null;
                    return new ResolvedMetadata(
                            Metadata.from(entity, logo, properties),
                            new Standards(Cip26TokenMetadata.from(entity, logo), null));
                });
    }

    private Optional<ResolvedMetadata> findCip68Metadata(String subject, List<String> properties) {
        return cip68StorageReader.findBySubject(subject, properties)
                .map(cip68TokenMetadata -> new ResolvedMetadata(
                        Metadata.from(cip68TokenMetadata),
                        new Standards(null, cip68TokenMetadata)));
    }

    private static BinaryOperator<ResolvedMetadata> aggregateResults() {
        return (thisPair, thatPair) -> {
            Metadata metadata = thisPair.metadata().merge(thatPair.metadata());
            Standards standards = thisPair.standards().merge(thatPair.standards());
            return new ResolvedMetadata(metadata, standards);
        };
    }

    private BiFunction<ResolvedMetadata, QueryPriority, ResolvedMetadata> combineStandards(String subject, List<String> properties) {
        return (accumulated, priority) -> findMetadata(subject, properties, priority)
                .map(found -> new ResolvedMetadata(
                        accumulated.metadata().merge(found.metadata()),
                        accumulated.standards().merge(found.standards())))
                .orElse(accumulated);
    }

    private BiFunction<ResolvedMetadata, QueryPriority, ResolvedMetadata> combineStandardsBatch(
            String subject, List<String> properties, BatchPrefetchData prefetchData) {
        return (accumulated, priority) -> findMetadataBatch(subject, properties, priority, prefetchData)
                .map(found -> new ResolvedMetadata(
                        accumulated.metadata().merge(found.metadata()),
                        accumulated.standards().merge(found.standards())))
                .orElse(accumulated);
    }

    private Optional<ResolvedMetadata> findMetadataBatch(String subject, List<String> properties,
                                                               QueryPriority priority, BatchPrefetchData prefetchData) {
        return switch (priority) {
            case CIP_26 -> findCip26MetadataBatch(subject, properties, prefetchData);
            case CIP_68 -> findCip68MetadataBatch(subject, prefetchData);
        };
    }

    private Optional<ResolvedMetadata> findCip26MetadataBatch(String subject, List<String> properties,
                                                                    BatchPrefetchData prefetchData) {
        TokenMetadata entity = prefetchData.cip26MetadataMap().get(subject);
        if (entity == null) {
            return Optional.empty();
        }
        String logo = (properties.isEmpty() || properties.contains("logo"))
                ? prefetchData.cip26LogoMap().get(subject)
                : null;
        return Optional.of(new ResolvedMetadata(
                Metadata.from(entity, logo, properties),
                new Standards(Cip26TokenMetadata.from(entity, logo), null)));
    }

    /**
     * Looks up CIP-68 metadata from the pre-fetched batch map keyed by fungible-token subject.
     * Property filtering was already applied when the map was built in {@link #prefetchBatch}.
     */
    private Optional<ResolvedMetadata> findCip68MetadataBatch(String subject, BatchPrefetchData prefetchData) {
        FungibleTokenMetadata cip68TokenMetadata = prefetchData.cip68MetadataMap().get(subject);
        if (cip68TokenMetadata == null) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedMetadata(
                Metadata.from(cip68TokenMetadata),
                new Standards(null, cip68TokenMetadata)));
    }

    private Map<String, Extension> buildExtensions(String subject) {
        Map<String, Extension> extensions = new LinkedHashMap<>();
        cip113StorageReader.findByPolicyId(AssetType.fromUnit(subject).policyId())
                .ifPresent(cip113 -> extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113));
        return extensions;
    }

    /**
     * Merged result of a query-priority reduce: the {@link Metadata} view assembled from one
     * or more standards, plus the raw per-standard {@link Standards} breakdown. Named after
     * its role (the resolved metadata for a subject) rather than "pair" so the accessors
     * {@link #metadata()} and {@link #standards()} read at call sites.
     */
    private record ResolvedMetadata(Metadata metadata, Standards standards) {
    }
}
