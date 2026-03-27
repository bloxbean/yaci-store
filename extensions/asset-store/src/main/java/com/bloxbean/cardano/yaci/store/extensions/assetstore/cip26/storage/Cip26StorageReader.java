package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to CIP-26 offchain token metadata.
 */
public interface Cip26StorageReader {

    Optional<TokenMetadata> findBySubject(String subject);

    List<TokenMetadata> findBySubjects(List<String> subjects);

    Optional<String> findLogoBySubject(String subject);
}
