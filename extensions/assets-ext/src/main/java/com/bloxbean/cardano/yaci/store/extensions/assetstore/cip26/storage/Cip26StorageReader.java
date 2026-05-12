package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only access to CIP-26 offchain fungible token metadata.
 */
public interface Cip26StorageReader {

    /**
     * Find metadata for a single subject.
     */
    Optional<Cip26Metadata> findBySubject(String subject);

    /**
     * Batch lookup metadata for multiple subjects.
     */
    List<Cip26Metadata> findBySubjects(List<String> subjects);

    /**
     * Find the logo (base64-encoded PNG) for a subject.
     */
    Optional<String> findLogoBySubject(String subject);

    /**
     * Batch lookup logos for multiple subjects. Returns a map of subject → logo (base64).
     * Subjects without logos are omitted from the map.
     */
    Map<String, String> findLogosBySubjects(List<String> subjects);

}
