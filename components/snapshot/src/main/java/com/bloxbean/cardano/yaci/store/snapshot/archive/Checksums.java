package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code SHA256SUMS} in the usual {@code sha256sum -c} format: every part and the manifest, so a
 * snapshot can be verified with standard tools before any Yaci Store code runs. It is also what a
 * snapshot's signature covers (ADR 0005, D9).
 */
public final class Checksums {

    public static final String FILE = "SHA256SUMS";

    private Checksums() {
    }

    /** Writes {@code SHA256SUMS} next to the manifest and returns it. */
    public static Path write(SnapshotManifest manifest, Path manifestPath) throws IOException {
        Map<String, String> sums = new TreeMap<>();
        manifest.parts().forEach(p -> sums.put(p.fileName(), p.sha256()));
        sums.put(manifestPath.getFileName().toString(), Digests.sha256Hex(manifestPath));
        StringBuilder sb = new StringBuilder();
        sums.forEach((name, sha) -> sb.append(sha).append("  ").append(name).append('\n'));
        Path file = manifestPath.toAbsolutePath().getParent().resolve(FILE);
        Files.writeString(file, sb.toString());
        return file;
    }

    /** File name to SHA-256, as listed. */
    public static Map<String, String> parse(String text) {
        Map<String, String> out = new TreeMap<>();
        for (String line : text.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            int sep = line.indexOf("  ");
            if (sep != 64) {
                throw new IllegalArgumentException("Malformed " + FILE + " line: " + line);
            }
            out.put(line.substring(sep + 2).trim(), line.substring(0, sep));
        }
        return out;
    }
}
