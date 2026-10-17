package com.bloxbean.cardano.yaci.store.snapshot.remote;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * {@code <network>/index.json}: the snapshots published for one network, newest first (ADR 0005, D4).
 *
 * <p>It is only for discovery. Every snapshot it points to is verified on its own: the manifest
 * against the digest recorded here and, when signed, against its signature.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SnapshotIndex(int formatVersion, String network, long protocolMagic, String updatedAt,
                            List<Entry> snapshots) {

    public static final int FORMAT_VERSION = 1;
    public static final String FILE = "index.json";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /**
     * One published snapshot.
     *
     * @param manifest       key of the manifest, relative to the network directory
     * @param manifestSha256 SHA-256 of the manifest bytes; a downloaded manifest must match it
     * @param keyId          the signing key's id, or null for an unsigned snapshot
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String snapshotId, String flavor, Map<String, String> retention, int epoch, long slot,
                        long blockNumber, String blockHash, String createdAt, String yaciStoreVersion,
                        int formatVersion, String specFormatVersion, String manifest, String manifestSha256,
                        long sizeBytes, int parts, List<String> modules, String keyId) {

        public static Entry of(SnapshotManifest m, String manifestKey, String manifestSha256, String keyId) {
            return new Entry(m.snapshotId(), m.flavorOrFull(), m.retention() == null || m.retention().isEmpty() ? null : m.retention(),
                    m.point().epoch(), m.point().slot(), m.point().blockNumber(), m.point().blockHash(), m.createdAt(),
                    m.yaciStoreVersion(), m.formatVersion(), m.specFormatVersion(), manifestKey, manifestSha256,
                    m.parts().stream().mapToLong(SnapshotManifest.PartManifest::size).sum(), m.parts().size(),
                    m.modules(), keyId);
        }

        public String flavorOrFull() {
            return flavor == null ? SnapshotManifest.FLAVOR_FULL : flavor;
        }

        /** The directory the snapshot's files are in, relative to the network directory. */
        public String directory() {
            int slash = manifest.lastIndexOf('/');
            return slash < 0 ? "" : manifest.substring(0, slash + 1);
        }
    }

    public static SnapshotIndex empty(String network, long protocolMagic) {
        return new SnapshotIndex(FORMAT_VERSION, network, protocolMagic, null, List.of());
    }

    /** A copy with {@code entry} added (replacing one with the same id), newest first. */
    public SnapshotIndex with(Entry entry, String now) {
        List<Entry> out = new ArrayList<>(snapshots == null ? List.of() : snapshots);
        out.removeIf(e -> e.snapshotId().equals(entry.snapshotId()));
        out.add(entry);
        return new SnapshotIndex(FORMAT_VERSION, network, protocolMagic, now, sorted(out));
    }

    public static List<Entry> sorted(List<Entry> entries) {
        List<Entry> out = new ArrayList<>(entries);
        out.sort(Comparator.comparingLong(Entry::slot).thenComparing(Entry::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .reversed());
        return out;
    }

    public byte[] toBytes() {
        try {
            return (MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this) + "\n").getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SnapshotIndex parse(byte[] json) {
        try {
            SnapshotIndex index = MAPPER.readValue(json, SnapshotIndex.class);
            if (index.formatVersion() > FORMAT_VERSION) {
                throw new IllegalStateException("index.json format " + index.formatVersion()
                        + " is newer than this release understands (" + FORMAT_VERSION + "); upgrade Yaci Store");
            }
            return index;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to parse index.json", e);
        }
    }
}
