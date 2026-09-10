package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Result of diffing the mappings folder between two commits of the token registry repository.
 *
 * @param upsertedFiles    mapping files that were added or modified and must be (re-)indexed
 * @param deletedFileNames file names (e.g. {@code <subject>.json}) of mapping files that were
 *                         removed from the registry and whose metadata must be deleted locally
 */
public record ChangedMappings(List<Path> upsertedFiles, List<String> deletedFileNames) {

    public static ChangedMappings empty() {
        return new ChangedMappings(List.of(), List.of());
    }

}
