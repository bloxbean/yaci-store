package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;

/**
 * Where an import reads its parts from: a local directory, or a remote store streamed straight into
 * the extractor (ADR 0005, D5). Every byte read is checked against the manifest either way.
 */
public interface PartSource {

    /** A short description for progress and errors, without credentials. */
    String describe();

    /** True when a read can fail transiently, so a failed part is worth retrying. */
    boolean remote();

    /** The part's size, or empty when it is not there. */
    OptionalLong size(SnapshotManifest.PartManifest part) throws IOException;

    /** The part's bytes from the start. The caller closes the stream. */
    InputStream open(SnapshotManifest.PartManifest part) throws IOException;

    /** The parts in a local directory, as export wrote them. */
    static PartSource directory(Path dir) {
        return new PartSource() {
            @Override
            public String describe() {
                return dir.toString();
            }

            @Override
            public boolean remote() {
                return false;
            }

            @Override
            public OptionalLong size(SnapshotManifest.PartManifest part) throws IOException {
                Path p = dir.resolve(part.fileName());
                return Files.isRegularFile(p) ? OptionalLong.of(Files.size(p)) : OptionalLong.empty();
            }

            @Override
            public InputStream open(SnapshotManifest.PartManifest part) throws IOException {
                return Files.newInputStream(dir.resolve(part.fileName()));
            }
        };
    }
}
