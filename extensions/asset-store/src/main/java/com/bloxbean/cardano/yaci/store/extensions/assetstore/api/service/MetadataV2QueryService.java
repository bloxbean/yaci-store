package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2.*;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service.Cip113RegistryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository.TokenLogoRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository.TokenMetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68FungibleTokenService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

/**
 * Core query and merge logic for the V2 metadata API.
 * <p>
 * Merges CIP-26 and CIP-68 metadata based on a configurable priority order,
 * and appends CIP-113 extensions when applicable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataV2QueryService {

    private static final MetadataStandardsPair IDENTITY = new MetadataStandardsPair(Metadata.empty(), Standards.empty());

    private final TokenMetadataRepository tokenMetadataRepository;
    private final TokenLogoRepository tokenLogoRepository;
    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final Cip113RegistryService cip113RegistryService;

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
        return new Subject(subject, pair.metadata(),
                showCipsDetails ? pair.standards() : null,
                extensions.isEmpty() ? null : extensions);
    }

    /**
     * Query and merge metadata for a single subject in batch context,
     * using a pre-fetched CIP-113 map to avoid N+1 queries.
     *
     * @param subject           the subject identifier
     * @param queryPriority     ordered list of CIP standards to query
     * @param queryProperties   list of properties to include (empty means all)
     * @param cip113Map         pre-fetched CIP-113 data keyed by policy ID
     * @param showCipsDetails   whether to include raw per-standard metadata
     * @return the merged Subject (may have empty/invalid metadata -- caller should filter)
     */
    public Subject querySubjectBatch(String subject,
                                     List<QueryPriority> queryPriority,
                                     List<String> queryProperties,
                                     Map<String, ProgrammableTokenCip113> cip113Map,
                                     boolean showCipsDetails) {

        MetadataStandardsPair pair = queryPriority.stream()
                .reduce(IDENTITY, combineStandards(subject, queryProperties), aggregateResults());

        Map<String, Extension> extensions = new LinkedHashMap<>();
        ProgrammableTokenCip113 cip113 = cip113Map.get(AssetType.fromUnit(subject).policyId());
        if (cip113 != null) {
            extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113);
        }

        return new Subject(subject, pair.metadata(),
                showCipsDetails ? pair.standards() : null,
                extensions.isEmpty() ? null : extensions);
    }

    /**
     * Pre-fetch CIP-113 data for a list of subjects to avoid N+1 queries.
     */
    public Map<String, ProgrammableTokenCip113> prefetchCip113(List<String> subjects) {
        List<String> policyIds = subjects.stream()
                .map(s -> AssetType.fromUnit(s).policyId())
                .distinct()
                .toList();
        return cip113RegistryService.findByPolicyIds(policyIds);
    }

    private Optional<MetadataStandardsPair> findMetadata(String subject, List<String> properties, QueryPriority priority) {
        return switch (priority) {
            case CIP_26 -> findCip26Metadata(subject, properties);
            case CIP_68 -> findCip68Metadata(subject, properties);
        };
    }

    private Optional<MetadataStandardsPair> findCip26Metadata(String subject, List<String> properties) {
        return tokenMetadataRepository.findById(subject)
                .map(entity -> {
                    String logo = tokenLogoRepository.findById(subject)
                            .map(tokenLogo -> tokenLogo.getLogo())
                            .orElse(null);
                    return new MetadataStandardsPair(
                            Metadata.from(entity, logo, properties),
                            new Standards(entity, null));
                });
    }

    private Optional<MetadataStandardsPair> findCip68Metadata(String subject, List<String> properties) {
        return cip68FungibleTokenService.getReferenceNftSubject(subject)
                .flatMap(assetType -> cip68FungibleTokenService.findSubject(assetType.policyId(), assetType.assetName(), properties))
                .map(fungibleTokenMetadata -> new MetadataStandardsPair(
                        Metadata.from(fungibleTokenMetadata),
                        new Standards(null, fungibleTokenMetadata)));
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

    private Map<String, Extension> buildExtensions(String subject) {
        Map<String, Extension> extensions = new LinkedHashMap<>();
        cip113RegistryService.findByPolicyId(AssetType.fromUnit(subject).policyId())
                .ifPresent(cip113 -> extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113));
        return extensions;
    }

    /**
     * Internal pair used during the reduce/merge algorithm.
     */
    private record MetadataStandardsPair(Metadata metadata, Standards standards) {
    }

}
