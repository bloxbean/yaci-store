package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.*;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class TokenQueryService {

    private static final MetadataStandardsPair IDENTITY = new MetadataStandardsPair(Metadata.empty(), Standards.empty());

    private final Cip26StorageReader cip26StorageReader;
    private final Cip68StorageReader cip68StorageReader;
    private final Optional<Cip113StorageReader> cip113StorageReader;

    @Autowired
    public TokenQueryService(Cip26StorageReader cip26StorageReader,
                                   Cip68StorageReader cip68StorageReader,
                                   @Autowired(required = false) Cip113StorageReader cip113StorageReader) {
        this.cip26StorageReader = cip26StorageReader;
        this.cip68StorageReader = cip68StorageReader;
        this.cip113StorageReader = Optional.ofNullable(cip113StorageReader);
    }

    /**
     * Query and merge metadata for a single subject.
     *
     * @param subject         the subject identifier (policyId + assetName hex)
     * @param queryPriority   ordered list of CIP standards to query
     * @param queryProperties list of properties to include (empty means all)
     * @param showCipsDetails whether to include raw per-standard metadata
     * @return the merged Subject, or null if no valid metadata found
     */
    @Nullable
    public Subject querySubject(String subject,
                                List<QueryPriority> queryPriority,
                                List<String> queryProperties,
                                boolean showCipsDetails) {

        MetadataStandardsPair pair = queryPriority.stream()
                .reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());

        if (pair.metadata().isEmpty() || !pair.metadata().isValid()) {
            return null;
        }

        Map<String, Extension> extensions = buildExtensions(subject);
        TokenType type = extensions.isEmpty() ? TokenType.NATIVE : TokenType.PROGRAMMABLE;
        return new Subject(subject, type, pair.metadata(),
                showCipsDetails ? pair.standards() : null,
                extensions.isEmpty() ? null : extensions);
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

        MetadataStandardsPair pair = queryPriority.stream()
                .reduce(IDENTITY, combineStandardsBatch(subject, queryProperties, prefetchData), aggregateResults());

        Map<String, Extension> extensions = new LinkedHashMap<>();
        ProgrammableTokenCip113 cip113 = prefetchData.cip113Map().get(AssetType.fromUnit(subject).policyId());
        if (cip113 != null) {
            extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113);
        }

        TokenType type = extensions.isEmpty() ? TokenType.NATIVE : TokenType.PROGRAMMABLE;
        return new Subject(subject, type, pair.metadata(),
                showCipsDetails ? pair.standards() : null,
                extensions.isEmpty() ? null : extensions);
    }

    /**
     * Pre-fetch all data for a batch of subjects in bulk queries to avoid N+1.
     * Issues at most 4 queries regardless of batch size:
     * CIP-26 metadata, CIP-26 logos, CIP-68 (per-subject, no batch yet), CIP-113.
     */
    public BatchPrefetchData prefetchBatch(List<String> subjects, List<String> queryProperties) {
        // CIP-113: one query for all distinct policy IDs
        Map<String, ProgrammableTokenCip113> cip113Map = cip113StorageReader.map(reader -> {
            List<String> policyIds = subjects.stream()
                    .map(s -> AssetType.fromUnit(s).policyId())
                    .distinct()
                    .toList();
            return reader.findByPolicyIds(policyIds);
        }).orElse(Map.of());

        // CIP-26: one query for all metadata, one query for all logos
        Map<String, TokenMetadata> cip26MetadataMap = cip26StorageReader.findBySubjects(subjects).stream()
                .collect(Collectors.toMap(TokenMetadata::getSubject, Function.identity()));

        boolean needLogos = queryProperties.isEmpty() || queryProperties.contains("logo");
        Map<String, String> cip26LogoMap = needLogos
                ? cip26StorageReader.findLogosBySubjects(subjects)
                : Map.of();

        return new BatchPrefetchData(cip113Map, cip26MetadataMap, cip26LogoMap);
    }

    /**
     * Pre-fetched data for batch operations. Eliminates N+1 queries.
     */
    public record BatchPrefetchData(
            Map<String, ProgrammableTokenCip113> cip113Map,
            Map<String, TokenMetadata> cip26MetadataMap,
            Map<String, String> cip26LogoMap) {
    }

    private Optional<MetadataStandardsPair> findMetadata(String subject, List<String> properties, QueryPriority priority) {
        return switch (priority) {
            case CIP_26 -> findCip26Metadata(subject, properties);
            case CIP_68 -> findCip68Metadata(subject, properties);
        };
    }

    private Optional<MetadataStandardsPair> findCip26Metadata(String subject, List<String> properties) {
        return cip26StorageReader.findBySubject(subject)
                .map(entity -> {
                    // Only fetch logo if all properties requested or logo explicitly requested
                    String logo = (properties.isEmpty() || properties.contains("logo"))
                            ? cip26StorageReader.findLogoBySubject(subject).orElse(null)
                            : null;
                    return new MetadataStandardsPair(
                            Metadata.from(entity, logo, properties),
                            new Standards(entity, null));
                });
    }

    private Optional<MetadataStandardsPair> findCip68Metadata(String subject, List<String> properties) {
        return cip68StorageReader.findBySubject(subject, properties)
                .map(cip68TokenMetadata -> new MetadataStandardsPair(
                        Metadata.from(cip68TokenMetadata),
                        new Standards(null, cip68TokenMetadata)));
    }

    private static BinaryOperator<MetadataStandardsPair> aggregateResults() {
        return (thisPair, thatPair) -> {
            Metadata metadata = thisPair.metadata().merge(thatPair.metadata());
            Standards standards = thisPair.standards().merge(thatPair.standards());
            return new MetadataStandardsPair(metadata, standards);
        };
    }

    private BiFunction<MetadataStandardsPair, QueryPriority, MetadataStandardsPair> combineStandards(String subject, List<String> properties) {
        return (accumulated, priority) -> findMetadata(subject, properties, priority)
                .map(found -> new MetadataStandardsPair(
                        accumulated.metadata().merge(found.metadata()),
                        accumulated.standards().merge(found.standards())))
                .orElse(accumulated);
    }

    private BiFunction<MetadataStandardsPair, QueryPriority, MetadataStandardsPair> combineStandardsBatch(
            String subject, List<String> properties, BatchPrefetchData prefetchData) {
        return (accumulated, priority) -> findMetadataBatch(subject, properties, priority, prefetchData)
                .map(found -> new MetadataStandardsPair(
                        accumulated.metadata().merge(found.metadata()),
                        accumulated.standards().merge(found.standards())))
                .orElse(accumulated);
    }

    private Optional<MetadataStandardsPair> findMetadataBatch(String subject, List<String> properties,
                                                               QueryPriority priority, BatchPrefetchData prefetchData) {
        return switch (priority) {
            case CIP_26 -> findCip26MetadataBatch(subject, properties, prefetchData);
            case CIP_68 -> findCip68Metadata(subject, properties); // CIP-68 is per-subject (label-filtered)
        };
    }

    private Optional<MetadataStandardsPair> findCip26MetadataBatch(String subject, List<String> properties,
                                                                    BatchPrefetchData prefetchData) {
        TokenMetadata entity = prefetchData.cip26MetadataMap().get(subject);
        if (entity == null) {
            return Optional.empty();
        }
        String logo = (properties.isEmpty() || properties.contains("logo"))
                ? prefetchData.cip26LogoMap().get(subject)
                : null;
        return Optional.of(new MetadataStandardsPair(
                Metadata.from(entity, logo, properties),
                new Standards(entity, null)));
    }

    private Map<String, Extension> buildExtensions(String subject) {
        Map<String, Extension> extensions = new LinkedHashMap<>();
        cip113StorageReader.flatMap(reader -> reader.findByPolicyId(AssetType.fromUnit(subject).policyId()))
                .ifPresent(cip113 -> extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113));
        return extensions;
    }

    /**
     * Internal pair used during the reduce/merge algorithm.
     */
    private record MetadataStandardsPair(Metadata metadata, Standards standards) {
    }
}
