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
 * <p>An import adds a retention of its own: the newest epochs of {@code epoch_stake}
 * ({@code --epoch-stake-epochs}), and the current tier of each table whose history the snapshot packages
 * apart ({@link SnapshotManifest.HistorySplit}), when the target prunes that table ({@link #forTarget}).
 * {@link #apply} then gives the importer and the validator the same reduced view of the manifest, so
 * both expect exactly the kept files and rows.
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

    /**
     * The target setting that widens a pruning service's window, with its default, for a key whose
     * value is a window; null for any other key.
     */
    public static Map.Entry<String, String> windowSetting(String key) {
        return switch (key) {
            case UTXO_SPENT_SAFE_BLOCKS, TX_INPUT_SAFE_BLOCKS -> Map.entry("store.utxo.pruning-safe-blocks", "2160");
            default -> null;
        };
    }

    /**
     * What an import of {@code archive} keeps: {@code requested}, plus the current tier of each table
     * with a history split whose pruning the target runs ({@link #requiredSetting}) with a window no
     * wider than the split's. A target with a wider window loads the history too, and its own pruning
     * service removes what it does not keep.
     *
     * @param notes receives, by target table, whether its split applies and why
     */
    public static Map<String, String> forTarget(SnapshotManifest archive, Map<String, String> targetSettings,
                                                Map<String, String> requested, Map<String, String> notes) {
        Map<String, String> out = new TreeMap<>(requested == null ? Map.of() : requested);
        for (SnapshotManifest.TableManifest t : archive.tables()) {
            SnapshotManifest.HistorySplit split = t.historySplit();
            if (split == null) {
                continue;
            }
            String setting = requiredSetting(split.key());
            if (setting == null || !"true".equalsIgnoreCase(targetSettings.getOrDefault(setting, "false"))) {
                notes.put(t.targetTable(), String.format("%s: full history (%s is off)", t.targetTable(), setting));
                continue;
            }
            Map.Entry<String, String> window = windowSetting(split.key());
            if (window != null) {
                long target = Long.parseLong(targetSettings.getOrDefault(window.getKey(), window.getValue()).trim());
                if (target > Long.parseLong(split.value())) {
                    notes.put(t.targetTable(), String.format("%s: full history, because %s=%d is wider than the snapshot's %s; the"
                            + " target's pruning removes the rest", t.targetTable(), window.getKey(), target, split.value()));
                    continue;
                }
            }
            out.put(split.key(), split.value());
            notes.put(t.targetTable(), String.format("%s: current rows only (%s=%s), its history parts are skipped",
                    t.targetTable(), split.key(), split.value()));
        }
        return out;
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
     * The manifest an import with {@code extra} sees: a {@code key=value} that matches a table's
     * {@link SnapshotManifest.HistorySplit} keeps its current tier, and each {@code <table>.epochs=N}
     * keeps that table's newest N epoch partitions. The manifest records the combined retention. Parts
     * are unchanged: a skipped file is simply not extracted, and a part holding only skipped files is
     * not read. Without {@code extra}, the manifest is returned as is.
     *
     * @throws IllegalArgumentException when an entry matches no table, or a table's files are not one
     *                                  epoch per partition, or its row count is not the sum of its
     *                                  files', so the kept rows could not be counted from the manifest
     */
    public static SnapshotManifest apply(SnapshotManifest manifest, Map<String, String> extra) {
        if (extra == null || extra.isEmpty()) {
            return manifest;
        }
        java.util.Set<String> used = new java.util.HashSet<>();
        List<SnapshotManifest.TableManifest> tables = new ArrayList<>();
        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            SnapshotManifest.TableManifest view = t;
            SnapshotManifest.HistorySplit split = t.historySplit();
            if (split != null && split.value().equals(extra.get(split.key()))) {
                view = currentTier(t, split);
                used.add(split.key());
            }
            String epochs = extra.get(t.targetTable() + EPOCHS);
            if (epochs != null) {
                used.add(t.targetTable() + EPOCHS);
                if (view.files() != null && !view.files().isEmpty()) {
                    view = keepNewestEpochs(view, Integer.parseInt(epochs));
                }
            }
            tables.add(view);
        }
        for (String key : extra.keySet()) {
            if (!used.contains(key) && !key.endsWith(EPOCHS)) {
                throw new IllegalArgumentException("Retention '" + key + "=" + extra.get(key) + "' cannot be applied"
                        + " at import: the snapshot has no history split for it");
            }
        }
        Map<String, String> combined = new TreeMap<>(manifest.retentionOrEmpty());
        combined.putAll(extra);
        return manifest.withTables(tables, manifest.flavor(), combined);
    }

    private static SnapshotManifest.TableManifest currentTier(SnapshotManifest.TableManifest t,
                                                              SnapshotManifest.HistorySplit split) {
        List<SnapshotManifest.FileEntry> current = t.files().stream().filter(f -> !f.inHistory()).toList();
        return t.reduced(current, split.rowCount(), split.bounds() == null ? Map.of() : split.bounds());
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
        return t.reduced(kept, rows, bounds);
    }
}
