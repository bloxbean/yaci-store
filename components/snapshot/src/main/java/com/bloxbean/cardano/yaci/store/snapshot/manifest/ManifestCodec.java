package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Canonical JSON serialisation of a {@link SnapshotManifest}.
 *
 * <p>Keys are sorted alphabetically and nulls are omitted, so two exports of the same pinned file
 * set produce byte-identical manifests and therefore identical digests.
 */
public class ManifestCodec {

    public static final int FORMAT_VERSION = 1;
    public static final String SPEC_FORMAT_VERSION = "1";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    private static final ObjectWriter WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    public String toJson(SnapshotManifest manifest) {
        try {
            return WRITER.writeValueAsString(manifest) + "\n";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] toBytes(SnapshotManifest manifest) {
        return toJson(manifest).getBytes(StandardCharsets.UTF_8);
    }

    public String digest(SnapshotManifest manifest) {
        return Digests.sha256Hex(toBytes(manifest));
    }

    public SnapshotManifest read(Path file) {
        try {
            return MAPPER.readValue(Files.readAllBytes(file), SnapshotManifest.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read manifest " + file, e);
        }
    }

    public SnapshotManifest parse(byte[] json) {
        try {
            return MAPPER.readValue(json, SnapshotManifest.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to parse manifest", e);
        }
    }

    /**
     * Publish the manifest last, via a temporary file and an atomic rename, so a partially written
     * manifest can never be mistaken for a complete snapshot.
     */
    public void writeAtomically(SnapshotManifest manifest, Path target) {
        try {
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(tmp, toBytes(manifest));
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write manifest " + target, e);
        }
    }
}
