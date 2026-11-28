package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import java.util.List;
import java.util.Map;

/**
 * The pruning a snapshot source ran with, and why a source must not prune at all.
 *
 * <p>A snapshot is built from the analytics export, which writes each day (or epoch) some time after it
 * ends. A pruning service deletes rows from the store before that export reads them: UTxO pruning, for
 * example, deletes a spent output together with its spend about 2160 blocks after the spend. The spend
 * then never reaches the export, while the output was already exported on the day it was created, so the
 * snapshot restores a spent output as unspent. Every snapshot packages the full history (the importer
 * picks the tiers a pruning target keeps), so its source must keep every row: export and import refuse
 * a source with any {@code *pruning-enabled} setting on, or with {@code store.account.history-cleanup-enabled},
 * which deletes older address_balance and stake_address_balance history rows the same way.
 */
public final class SourcePruning {

    private static final String SUFFIX = "pruning-enabled";

    /** Deletes balance history like account pruning does, without the pruning-enabled suffix. */
    public static final String ACCOUNT_HISTORY_CLEANUP = "store.account.history-cleanup-enabled";

    private SourcePruning() {
    }

    /** The {@code *pruning-enabled} settings (and the history cleanup) that are on, as {@code key=true}, sorted. */
    public static List<String> enabled(Map<String, String> settings) {
        if (settings == null) {
            return List.of();
        }
        return settings.entrySet().stream()
                .filter(e -> (e.getKey().endsWith(SUFFIX) || e.getKey().equals(ACCOUNT_HISTORY_CLEANUP))
                        && "true".equalsIgnoreCase(String.valueOf(e.getValue()).trim()))
                .map(e -> e.getKey() + "=true")
                .sorted()
                .toList();
    }

    /** Why an export of a source running with {@code enabled} is refused. */
    public static String exportBlocker(List<String> enabled) {
        return "The source runs with " + String.join(", ", enabled) + ". Pruning deletes rows (spent outputs,"
                + " their spends, old history) before the analytics export reads them, so the snapshot would"
                + " restore spent outputs as unspent. A snapshot needs a source that never prunes: sync it from"
                + " genesis with every pruning setting off.";
    }

    /** Why an import of a snapshot whose source ran with {@code enabled} is refused. */
    public static String importBlocker(List<String> enabled) {
        return "The snapshot was exported from a source that ran with " + String.join(", ", enabled)
                + ". Pruning on the source can drop spends before they are exported, so the snapshot may restore"
                + " spent outputs as unspent. Use a snapshot exported from a source that never prunes.";
    }
}
