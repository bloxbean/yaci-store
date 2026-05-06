package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fungible-token-facing implementation of {@link Cip68StorageReader}.
 * <p>
 * The {@code cip68_metadata} table stores rows for label 222 / 333 / 444; this reader
 * filters to label 333 (FT) only — the public API surface today is FT-focused. NFT/RFT
 * rows in the same table are dormant data, future-proofed for a later release that
 * adds NFT/RFT-specific service methods and DTOs.
 */
@RequiredArgsConstructor
public class Cip68StorageReaderImpl implements Cip68StorageReader {

    private final Cip68TokenService cip68TokenService;

    @Override
    public Optional<FungibleTokenMetadata> findByPolicyIdAndAssetName(String policyId, String assetName) {
        return cip68TokenService.findSubject(policyId, assetName, List.of());
    }

    @Override
    public Optional<FungibleTokenMetadata> findBySubject(String subject) {
        return findBySubject(subject, List.of());
    }

    @Override
    public Optional<FungibleTokenMetadata> findBySubject(String subject, List<String> properties) {
        return cip68TokenService.getReferenceNftSubject(subject)
                .flatMap(assetType -> cip68TokenService.findSubject(
                        assetType.policyId(), assetType.assetName(), properties));
    }

    @Override
    public Map<String, FungibleTokenMetadata> findBySubjects(List<String> subjects, List<String> properties) {
        if (subjects.isEmpty()) {
            return Map.of();
        }

        // Convert each fungible subject to its reference NFT key and remember the mapping back,
        // so the result can be keyed by the original fungible subject.
        List<AssetType> refNftKeys = new ArrayList<>(subjects.size());
        Map<String, String> fungibleSubjectByRefNftSubject = new HashMap<>(subjects.size());
        for (String subject : subjects) {
            cip68TokenService.getReferenceNftSubject(subject).ifPresent(refNftKey -> {
                refNftKeys.add(refNftKey);
                fungibleSubjectByRefNftSubject.put(refNftKey.toUnit(), subject);
            });
        }

        if (refNftKeys.isEmpty()) {
            return Map.of();
        }

        Map<String, FungibleTokenMetadata> byRefNftSubject = cip68TokenService.findSubjects(refNftKeys, properties);

        // Re-key the result by the original fungible subject so the caller doesn't need to
        // know about the prefix conversion.
        Map<String, FungibleTokenMetadata> result = new HashMap<>(byRefNftSubject.size());
        byRefNftSubject.forEach((refNftSubject, metadata) ->
                Optional.ofNullable(fungibleSubjectByRefNftSubject.get(refNftSubject))
                        .ifPresent(fungibleSubject -> result.put(fungibleSubject, metadata)));
        return result;
    }
}
