package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to CIP-26 offchain fungible token metadata.
 */
public interface Cip26StorageReader {

    /**
     * Find metadata for a single subject.
     */
    Optional<TokenMetadata> findBySubject(String subject);

    /**
     * Batch lookup metadata for multiple subjects.
     */
    List<TokenMetadata> findBySubjects(List<String> subjects);

    /**
     * Find the logo (base64-encoded PNG) for a subject.
     */
    Optional<String> findLogoBySubject(String subject);
}
