package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import jakarta.annotation.Nullable;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.*;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.Cip26TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
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
 * Merges CIP-26 and CIP-68 metadata based on a configurable priority order.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenQueryService {

    private static final ResolvedMetadata IDENTITY = new ResolvedMetadata(Metadata.empty(), Standards.empty(), List.of());

    private final Cip26StorageReader cip26StorageReader;
    private final Cip68StorageReader cip68StorageReader;

    /**
     * Query and merge metadata for a single subject.
     *
     * @param subject         the subject identifier (policyId + assetName hex)
     * @param queryPriority   ordered list of CIP standards to query
     * @param queryProperties list of properties to include (empty means all)
     * @param showCipsDetails whether to include raw per-standard metadata
     * @return the merged Subject, or empty if no standard has anything indexed for it. A subject that is indexed is
     *         returned as indexed, whatever is wrong with it: {@code validationErrors} says what
     */
    public Optional<Subject> querySubject(String subject,
                                          List<QueryPriority> queryPriority,
                                          List<String> queryProperties,
                                          boolean showCipsDetails) {

        ResolvedMetadata resolved = queryPriority.stream()
                .reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());

        if (resolved.standards().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toSubject(subject, resolved, showCipsDetails));
    }

    /**
     * Query and merge metadata for a single subject in batch context,
     * using pre-fetched data maps to avoid N+1 queries.
     *
     * @return the merged Subject, or empty if no standard has anything indexed for it
     */
    public Optional<Subject> querySubjectBatch(String subject,
                                               List<QueryPriority> queryPriority,
                                               List<String> queryProperties,
                                               BatchPrefetchData prefetchData,
                                               boolean showCipsDetails) {

        ResolvedMetadata resolved = queryPriority.stream()
                .reduce(IDENTITY, combineStandardsBatch(subject, queryProperties, prefetchData), aggregateResults());

        return resolved.standards().isEmpty()
                ? Optional.empty()
                : Optional.of(toSubject(subject, resolved, showCipsDetails));
    }

    private static Subject toSubject(String subject, ResolvedMetadata resolved, boolean showCipsDetails) {
        // valid: the token follows its CIP completely, mandatory and optional fields alike. A row indexed before validation
        // existed has no errors to look at (null, not empty): the rule it always had applies, name and description present.
        List<ValidationError> errors = resolved.validationErrors();
        boolean valid = errors != null
                ? errors.isEmpty()
                : resolved.metadata().name() != null && resolved.metadata().description() != null;
        Metadata metadata = resolved.metadata().toBuilder().valid(valid).build();
        return new Subject(subject, metadata, showCipsDetails ? resolved.standards() : null, resolved.validationErrors());
    }

    /**
     * Pre-fetch all data for a batch of subjects in bulk queries to avoid N+1.
     * Issues at most 3 queries regardless of batch size:
     * CIP-26 metadata, CIP-26 logos, CIP-68 reference NFTs.
     */
    public BatchPrefetchData prefetchBatch(List<String> subjects, List<String> queryProperties) {
        // CIP-26: one query for all metadata, one query for all logos
        Map<String, Cip26Metadata> cip26MetadataMap = cip26StorageReader.findBySubjects(subjects).stream()
                .collect(Collectors.toMap(Cip26Metadata::getSubject, Function.identity()));

        boolean needLogos = queryProperties.isEmpty() || queryProperties.contains("logo");
        Map<String, String> cip26LogoMap = needLogos
                ? cip26StorageReader.findLogosBySubjects(subjects)
                : Map.of();

        // CIP-68: one query for all fungible token subjects, result keyed by the original subject.
        // Non-fungible subjects are silently filtered inside findBySubjects, so passing the full
        // batch is safe and avoids a double prefix-check here.
        Map<String, FungibleTokenMetadata> cip68MetadataMap = cip68StorageReader.findBySubjects(subjects, queryProperties);

        return new BatchPrefetchData(cip26MetadataMap, cip26LogoMap, cip68MetadataMap);
    }

    /**
     * Pre-fetched data for batch operations. Eliminates N+1 queries.
     */
    public record BatchPrefetchData(
            Map<String, Cip26Metadata> cip26MetadataMap,
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
                    Cip26TokenMetadata cip26 = Cip26TokenMetadata.from(entity, logo);
                    return new ResolvedMetadata(
                            Metadata.from(entity, logo, properties),
                            new Standards(cip26, null),
                            cip26.getValidationErrors());
                });
    }

    private Optional<ResolvedMetadata> findCip68Metadata(String subject, List<String> properties) {
        return cip68StorageReader.findBySubject(subject, properties)
                .map(cip68TokenMetadata -> new ResolvedMetadata(
                        Metadata.from(cip68TokenMetadata),
                        new Standards(null, cip68TokenMetadata),
                        cip68TokenMetadata.validationErrors()));
    }

    private static BinaryOperator<ResolvedMetadata> aggregateResults() {
        return ResolvedMetadata::merge;
    }

    private BiFunction<ResolvedMetadata, QueryPriority, ResolvedMetadata> combineStandards(String subject, List<String> properties) {
        return (accumulated, priority) -> findMetadata(subject, properties, priority)
                .map(accumulated::merge)
                .orElse(accumulated);
    }

    private BiFunction<ResolvedMetadata, QueryPriority, ResolvedMetadata> combineStandardsBatch(
            String subject, List<String> properties, BatchPrefetchData prefetchData) {
        return (accumulated, priority) -> findMetadataBatch(subject, properties, priority, prefetchData)
                .map(accumulated::merge)
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
        Cip26Metadata entity = prefetchData.cip26MetadataMap().get(subject);
        if (entity == null) {
            return Optional.empty();
        }
        String logo = (properties.isEmpty() || properties.contains("logo"))
                ? prefetchData.cip26LogoMap().get(subject)
                : null;
        Cip26TokenMetadata cip26 = Cip26TokenMetadata.from(entity, logo);
        return Optional.of(new ResolvedMetadata(
                Metadata.from(entity, logo, properties),
                new Standards(cip26, null),
                cip26.getValidationErrors()));
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
                new Standards(null, cip68TokenMetadata),
                cip68TokenMetadata.validationErrors()));
    }

    /**
     * Merged result of a query-priority reduce: the {@link Metadata} view assembled from one
     * or more standards, plus the raw per-standard {@link Standards} breakdown. Named after
     * its role (the resolved metadata for a subject) rather than "pair" so the accessors
     * {@link #metadata()} and {@link #standards()} read at call sites.
     */
    private record ResolvedMetadata(Metadata metadata, Standards standards, @Nullable List<ValidationError> validationErrors) {

        /** The metadata of the first standard wins per property; the validation errors of every standard are kept. */
        ResolvedMetadata merge(ResolvedMetadata that) {
            List<ValidationError> errors = new ArrayList<>();
            if (validationErrors != null) {
                errors.addAll(validationErrors);
            }
            if (that.validationErrors() != null) {
                errors.addAll(that.validationErrors());
            }
            // unchecked (null) when a standard has no errors recorded and nothing else reports any: "no errors" would be a claim
            boolean unchecked = validationErrors == null || that.validationErrors() == null;
            return new ResolvedMetadata(metadata.merge(that.metadata()), standards.merge(that.standards()),
                    unchecked && errors.isEmpty() ? null : errors);
        }
    }

}
