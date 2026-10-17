package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a reduced snapshot keeps, as {@code <target table>.<rule>=<value>} entries (ADR 0005).
 *
 * <p>A pruned snapshot is built with its retention, and records it in the manifest. An import can add
 * an epoch retention of its own: {@link #apply} then gives the importer and the validator the same
 * reduced view of the manifest, so both expect exactly the kept files and rows.
 *
 * <p>Every retention matches a pruning service, and the target must run that service: otherwise it
 * would present partial history as complete. {@link #requiredSetting} names it.
 */
public final class Retention {

    /** Newest N epoch partitions of {@code epoch_stake}; {@code EpochStakePruningService}. */
    public static final String EPOCH_STAKE_EPOCHS = "epoch_stake.epochs";
    /** Spent outputs and their inputs older than N blocks before the point; {@code UtxoPruningService}. */
    public static final String UTXO_SPENT_SAFE_BLOCKS = "address_utxo.spent-safe-blocks";
    public static final String TX_INPUT_SAFE_BLOCKS = "tx_input.spent-safe-blocks";

    /** Reward calculation reads the stake of epochs E-3 to E; the pruning service refuses fewer. */
    public static final int MIN_EPOCH_STAKE_EPOCHS = 4;

    private static final String EPOCHS = ".epochs";
    private static final Pattern EPOCH_DIRECTORY = Pattern.compile("(?:^|/)epoch=(-?\\d+)(?:/|$)");

    private Retention() {
    }

    /** The store setting a target must enable to accept a snapshot with {@code key}, or null. */
    public static String requiredSetting(String key) {
        return switch (key) {
            case EPOCH_STAKE_EPOCHS -> "store.adapot.epoch-stake-pruning-enabled";
            case UTXO_SPENT_SAFE_BLOCKS, TX_INPUT_SAFE_BLOCKS -> "store.utxo.pruning-enabled";
            default -> null;
        };
    }

    /** The target table a retention entry reduces. */
    public static String table(String key) {
        int dot = key.indexOf('.');
        return dot < 0 ? key : key.substring(0, dot);
    }

    /** The retention an import adds for {@code --epoch-stake-epochs}; empty for 0 (keep every epoch). */
    public static Map<String, String> epochStake(int epochs) {
        if (epochs == 0) {
            return Map.of();
        }
        if (epochs < MIN_EPOCH_STAKE_EPOCHS) {
            throw new IllegalArgumentException("epoch_stake must keep at least " + MIN_EPOCH_STAKE_EPOCHS
                    + " epochs: reward calculation reads the stake of epochs E-3 to E");
        }
        return Map.of(EPOCH_STAKE_EPOCHS, Integer.toString(epochs));
    }

    /** Canonical text form, for the import journal: sorted {@code k=v} pairs joined by commas. */
    public static String format(Map<String, String> retention) {
        if (retention == null || retention.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(retention).forEach((k, v) -> sb.append(sb.isEmpty() ? "" : ",").append(k).append('=').append(v));
        return sb.toString();
    }

    public static Map<String, String> parse(String text) {
        Map<String, String> out = new TreeMap<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (String pair : text.split(",")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("Invalid retention entry '" + pair + "'");
            }
            out.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
        }
        return out;
    }

    /**
     * The manifest an import with {@code extra} sees: each {@code <table>.epochs=N} keeps that table's
     * newest N epoch partitions, and the manifest records the combined retention. Parts are unchanged:
     * a skipped file is simply not extracted. Without {@code extra}, the manifest is returned as is.
     *
     * @throws IllegalArgumentException when a table's files are not one epoch per partition, or its
     *                                  row count is not the sum of its files', so the kept rows could
     *                                  not be counted from the manifest alone
     */
    public static SnapshotManifest apply(SnapshotManifest manifest, Map<String, String> extra) {
        if (extra == null || extra.isEmpty()) {
            return manifest;
        }
        List<SnapshotManifest.TableManifest> tables = new ArrayList<>();
        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            String value = extra.get(t.targetTable() + EPOCHS);
            tables.add(value == null || t.files() == null || t.files().isEmpty() ? t
                    : keepNewestEpochs(t, Integer.parseInt(value)));
        }
        for (String key : extra.keySet()) {
            if (!key.endsWith(EPOCHS)) {
                throw new IllegalArgumentException("Retention '" + key + "' cannot be applied at import");
            }
        }
        Map<String, String> combined = new TreeMap<>(manifest.retentionOrEmpty());
        combined.putAll(extra);
        return manifest.withTables(tables, manifest.flavor(), combined);
    }

    private static SnapshotManifest.TableManifest keepNewestEpochs(SnapshotManifest.TableManifest t, int epochs) {
        long fileRows = t.files().stream().mapToLong(SnapshotManifest.FileEntry::rowCount).sum();
        if (fileRows != t.rowCount()) {
            throw new IllegalArgumentException("Cannot keep the newest epochs of " + t.targetTable()
                    + ": its " + t.rowCount() + " rows are not the " + fileRows + " rows of its files");
        }
        Map<SnapshotManifest.FileEntry, Integer> epochOf = new LinkedHashMap<>();
        for (SnapshotManifest.FileEntry f : t.files()) {
            Matcher m = EPOCH_DIRECTORY.matcher(f.path());
            if (!m.find()) {
                throw new IllegalArgumentException("Cannot keep the newest epochs of " + t.targetTable()
                        + ": " + f.path() + " is not in an epoch partition");
            }
            epochOf.put(f, Integer.parseInt(m.group(1)));
        }
        int newest = epochOf.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
        int oldestKept = newest - epochs + 1;
        List<SnapshotManifest.FileEntry> kept = t.files().stream().filter(f -> epochOf.get(f) >= oldestKept).toList();
        long rows = kept.stream().mapToLong(SnapshotManifest.FileEntry::rowCount).sum();

        Map<String, String> bounds = new TreeMap<>(t.bounds() == null ? Map.of() : t.bounds());
        if (bounds.containsKey("epoch.min")) {
            bounds.put("epoch.min", Integer.toString(Math.max(oldestKept,
                    kept.stream().mapToInt(epochOf::get).min().orElse(oldestKept))));
        }
        return new SnapshotManifest.TableManifest(t.specId(), t.specVersion(), t.specDigest(), t.module(),
                t.kind(), t.restore(), t.reason(), t.sourceRelation(), t.exporterId(), t.targetTable(),
                t.loadMode(), t.cutoffRule(), t.transformVersion(), t.dependencies(), rows, t.key(),
                t.columnFingerprint(), t.sourceColumns(), bounds, kept, t.contractDigest());
    }
}
