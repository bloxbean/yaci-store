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
 * {@code <network>/index.json}: the live snapshots of one network, newest first (ADR 0005, D4, D12).
 * It is public JSON, also meant for a listing page.
 *
 * <p>It is only for discovery. Every snapshot it points to is verified on its own: the manifest
 * against the digest recorded here and, when signed, against its signature. A snapshot that is
 * published but not yet live is not listed; its {@code <id>/snapshot.json} holds its entry, so it can
 * still be imported by its id for testing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SnapshotIndex(int formatVersion, String network, long protocolMagic, String updatedAt,
                            List<Entry> snapshots) {

    public static final int FORMAT_VERSION = 1;
    public static final String FILE = "index.json";
    /** Per-snapshot entry, next to the snapshot's files: what the index lists once it is live. */
    public static final String ENTRY_FILE = "snapshot.json";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /**
     * One published snapshot.
     *
     * @param manifest        key of the manifest, relative to the network directory
     * @param manifestSha256  SHA-256 of the manifest bytes; a downloaded manifest must match it
     * @param yaciStoreCommit git commit of the exporting release, when known
     * @param historySplits   {@code key=value} of each table whose history is in parts of its own, e.g.
     *                        {@code address_utxo.spent-safe-blocks=2160}; null when none
     * @param keyId           the signing key's id, or null for an unsigned snapshot
     * @param live            true once the snapshot is promoted into the index; false while staged
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String snapshotId, int epoch, long slot, long blockNumber, String blockHash, long blockTime,
                        String createdAt, String yaciStoreVersion, String yaciStoreCommit, int formatVersion,
                        String specFormatVersion, String manifest, String manifestSha256, long sizeBytes, int parts,
                        List<String> modules, Map<String, String> historySplits, String keyId, boolean live) {

        public static Entry of(SnapshotManifest m, String manifestKey, String manifestSha256, String keyId) {
            Map<String, String> splits = m.historySplits();
            return new Entry(m.snapshotId(), m.point().epoch(), m.point().slot(), m.point().blockNumber(),
                    m.point().blockHash(), m.point().blockTime(), m.createdAt(), m.yaciStoreVersion(), m.yaciStoreCommit(),
                    m.formatVersion(), m.specFormatVersion(), manifestKey, manifestSha256,
                    m.parts().stream().mapToLong(SnapshotManifest.PartManifest::size).sum(), m.parts().size(),
                    m.modules(), splits.isEmpty() ? null : splits, keyId, false);
        }

        /** A copy that is, or is not, live. */
        public Entry withLive(boolean isLive) {
            return new Entry(snapshotId, epoch, slot, blockNumber, blockHash, blockTime, createdAt, yaciStoreVersion,
                    yaciStoreCommit, formatVersion, specFormatVersion, manifest, manifestSha256, sizeBytes, parts,
                    modules, historySplits, keyId, isLive);
        }

        /** The release, with its commit when the version does not already name it. */
        public String release() {
            return yaciStoreCommit == null || yaciStoreCommit.isBlank()
                    || (yaciStoreVersion != null && yaciStoreVersion.contains(yaciStoreCommit))
                    ? yaciStoreVersion : yaciStoreVersion + " (" + yaciStoreCommit + ")";
        }

        /** The directory the snapshot's files are in, relative to the network directory. */
        public String directory() {
            int slash = manifest.lastIndexOf('/');
            return slash < 0 ? "" : manifest.substring(0, slash + 1);
        }

        public byte[] toBytes() {
            try {
                return (MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this) + "\n").getBytes(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public static Entry parse(byte[] json) {
            try {
                return MAPPER.readValue(json, Entry.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to parse " + ENTRY_FILE, e);
            }
        }
    }

    public static SnapshotIndex empty(String network, long protocolMagic) {
        return new SnapshotIndex(FORMAT_VERSION, network, protocolMagic, null, List.of());
    }

    /** A copy with {@code entry} added as live (replacing one with the same id), newest first. */
    public SnapshotIndex with(Entry entry, String now) {
        List<Entry> out = new ArrayList<>(snapshots == null ? List.of() : snapshots);
        out.removeIf(e -> e.snapshotId().equals(entry.snapshotId()));
        out.add(entry.withLive(true));
        return new SnapshotIndex(FORMAT_VERSION, network, protocolMagic, now, sorted(out));
    }

    /** A copy without the snapshot {@code snapshotId}. */
    public SnapshotIndex without(String snapshotId, String now) {
        List<Entry> out = new ArrayList<>(snapshots == null ? List.of() : snapshots);
        out.removeIf(e -> e.snapshotId().equals(snapshotId));
        return new SnapshotIndex(FORMAT_VERSION, network, protocolMagic, now, out);
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
