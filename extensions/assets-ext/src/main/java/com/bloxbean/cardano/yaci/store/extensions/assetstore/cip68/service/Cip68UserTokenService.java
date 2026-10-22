package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68UserToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.FUNGIBLE_TOKEN_PREFIX;
import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX;

/**
 * Answers whether a CIP-68 fungible (333) or rich fungible (444) user token has been seen on chain (ADR 0008). The API serves the
 * metadata of such a subject only when it has: the reference NFT of the same name holds the metadata, but it does not say that the user
 * token was ever minted.
 */
@Service
@RequiredArgsConstructor
public class Cip68UserTokenService {

    private final Cip68UserTokenRepository repository;

    public boolean exists(String unit) {
        return repository.existsById(normalize(unit));
    }

    /** Whether the fungible (333) or the rich fungible (444) token of this policy and name exists. {@code rawAssetName} has no label prefix. */
    public boolean existsAsFungibleOrRichFungible(String policyId, String rawAssetName) {
        return !existing(Set.of(policyId + FUNGIBLE_TOKEN_PREFIX + rawAssetName, policyId + RICH_FUNGIBLE_TOKEN_PREFIX + rawAssetName)).isEmpty();
    }

    /** The units of {@code units} that have been seen, in the same normal form (lower case) as the units that were asked for. One query. */
    public Set<String> existing(Collection<String> units) {
        if (units.isEmpty()) {
            return Set.of();
        }
        Set<String> wanted = units.stream().map(Cip68UserTokenService::normalize).collect(Collectors.toCollection(HashSet::new));
        return repository.findAllById(wanted).stream().map(Cip68UserToken::getUnit).collect(Collectors.toSet());
    }

    public static String normalize(String unit) {
        return unit.toLowerCase(Locale.ROOT);
    }
}
