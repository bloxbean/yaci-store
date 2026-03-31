package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Fungible token (label 333) implementation of {@link Cip68StorageReader}.
 * All queries are scoped to label 333 to avoid mixing with future NFT data.
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
}
