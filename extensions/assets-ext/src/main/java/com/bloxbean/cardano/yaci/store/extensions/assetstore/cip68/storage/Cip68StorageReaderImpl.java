package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fungible-token-facing implementation of {@link Cip68StorageReader}.
 * <p>
 * The {@code cip68_metadata} table stores rows for label 222 / 333 / 444; this reader
 * serves fungible (333) and rich fungible (444) token subjects, each checked for its own label.
 * NFTs (222) are not served.
 */
@RequiredArgsConstructor
public class Cip68StorageReaderImpl implements Cip68StorageReader {

    private final Cip68TokenService cip68TokenService;

    /** The reference NFT asked for directly is served as a fungible token (333): a 444 token is asked for by its subject. */
    @Override
    public Optional<FungibleTokenMetadata> findByPolicyIdAndAssetName(String policyId, String assetName) {
        return cip68TokenService.findSubject(policyId, assetName, Cip68Constants.LABEL_FT, List.of());
    }

    @Override
    public Optional<FungibleTokenMetadata> findBySubject(String subject) {
        return findBySubject(subject, List.of());
    }

    @Override
    public Optional<FungibleTokenMetadata> findBySubject(String subject, List<String> properties) {
        return cip68TokenService.getServedToken(subject)
                .flatMap(token -> cip68TokenService.findSubject(
                        token.referenceNft().policyId(), token.referenceNft().assetName(), token.label(), properties));
    }

    @Override
    public Map<String, FungibleTokenMetadata> findBySubjects(List<String> subjects, List<String> properties) {
        return cip68TokenService.findSubjects(subjects, properties);
    }
}
