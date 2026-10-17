package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetentionTest {

    private static SnapshotManifest.TableManifest table(String target, List<SnapshotManifest.FileEntry> files, long rows) {
        return new SnapshotManifest.TableManifest(target.replace('_', '-'), 1, "d", "adapot", "DERIVED_DATA", "IMPORT",
                null, target, target, target, "MAPPED", "EPOCH_LTE", 1, List.of(), rows, List.of("epoch"), "f",
                Map.of(), Map.of("epoch.min", "208", "epoch.max", "212"), files, "c");
    }

    private static SnapshotManifest manifest(SnapshotManifest.TableManifest... tables) {
        return new SnapshotManifest(1, "id", "t", "p", "v", "1", "duck", "1", null, null, List.of(), Map.of(),
                "s", "f", List.of(tables), List.of(), List.of());
    }

    private static List<SnapshotManifest.FileEntry> epochFiles(int from, int to) {
        List<SnapshotManifest.FileEntry> files = new ArrayList<>();
        for (int e = from; e <= to; e++) {
            files.add(new SnapshotManifest.FileEntry("main/epoch_stake/epoch=" + e + "/f.parquet", 10, "h", 100 + e,
                    "main/epoch_stake/epoch=" + e));
        }
        return files;
    }

    @Test
    void keepsTheNewestEpochPartitionsAndCountsTheirRows() {
        var files = epochFiles(208, 212);
        long rows = files.stream().mapToLong(SnapshotManifest.FileEntry::rowCount).sum();
        var other = table("block", List.of(), 0);
        SnapshotManifest view = Retention.apply(manifest(table("epoch_stake", files, rows), other),
                Retention.epochStake(4));

        var stake = view.tables().get(0);
        assertThat(stake.files()).extracting(SnapshotManifest.FileEntry::path)
                .allMatch(p -> !p.contains("epoch=208/")).hasSize(4);
        assertThat(stake.rowCount()).isEqualTo(309 + 310 + 311 + 312);
        assertThat(stake.bounds()).containsEntry("epoch.min", "209").containsEntry("epoch.max", "212");
        assertThat(view.tables().get(1)).isSameAs(other);
        assertThat(view.retention()).containsEntry(Retention.EPOCH_STAKE_EPOCHS, "4");
    }

    @Test
    void noRetentionLeavesTheManifestUnchanged() {
        SnapshotManifest m = manifest(table("epoch_stake", epochFiles(1, 3), 306));
        assertThat(Retention.apply(m, Retention.epochStake(0))).isSameAs(m);
        assertThat(Retention.format(Map.of())).isNull();
    }

    @Test
    void refusesFewerEpochsThanRewardCalculationReads() {
        assertThatThrownBy(() -> Retention.epochStake(3)).hasMessageContaining("at least 4");
    }

    @Test
    void refusesATableWhoseRowsAreNotTheSumOfItsFiles() {
        var files = epochFiles(1, 5);
        assertThatThrownBy(() -> Retention.apply(manifest(table("epoch_stake", files, 7)), Retention.epochStake(4)))
                .hasMessageContaining("not the");
    }

    @Test
    void formatsAndParsesCanonically() {
        Map<String, String> r = Map.of("tx_input.spent-safe-blocks", "2160", "epoch_stake.epochs", "8");
        assertThat(Retention.format(r)).isEqualTo("epoch_stake.epochs=8,tx_input.spent-safe-blocks=2160");
        assertThat(Retention.parse(Retention.format(r))).isEqualTo(r);
        assertThat(Retention.requiredSetting("epoch_stake.epochs")).isEqualTo("store.adapot.epoch-stake-pruning-enabled");
        assertThat(Retention.requiredSetting("address_utxo.spent-safe-blocks")).isEqualTo("store.utxo.pruning-enabled");
    }

    /** address_utxo with two original files (full-only) and two pruned copies, split at 2160 blocks. */
    private static SnapshotManifest.TableManifest splitUtxo() {
        List<SnapshotManifest.FileEntry> files = List.of(
                new SnapshotManifest.FileEntry("main/address_utxo/date=2026-01-02/pruned_a.parquet", 10, "h", 30, "main/address_utxo/date=2026-01-02", "pruned"),
                new SnapshotManifest.FileEntry("main/address_utxo/date=2026-01-03/pruned_b.parquet", 10, "h", 20, "main/address_utxo/date=2026-01-03", "pruned"),
                new SnapshotManifest.FileEntry("main/address_utxo/date=2026-01-02/ducklake-a.parquet", 10, "h", 400, "main/address_utxo/date=2026-01-02", "full"),
                new SnapshotManifest.FileEntry("main/address_utxo/date=2026-01-03/ducklake-b.parquet", 10, "h", 100, "main/address_utxo/date=2026-01-03", "full"));
        return new SnapshotManifest.TableManifest("address-utxo", 1, "d", "utxo", "CHAIN_DATA", "IMPORT", null,
                "address_utxo", "address_utxo", "address_utxo", "MAPPED", "SLOT_LTE", 1, List.of(), 200,
                List.of("tx_hash", "output_index"), "f", Map.of(), Map.of("slot.min", "0", "slot.max", "99"), files, "c",
                new SnapshotManifest.HistorySplit(Retention.UTXO_SPENT_SAFE_BLOCKS, "2160", 20,
                        Map.of("slot.min", "40", "slot.max", "99")));
    }

    @Test
    void aSplitSelectsThePrunedTierAndAFullImportNeverSeesIt() {
        var m = manifest(splitUtxo(), table("block", List.of(), 0));

        SnapshotManifest pruned = Retention.apply(m, Map.of(Retention.UTXO_SPENT_SAFE_BLOCKS, "2160"));
        SnapshotManifest full = Retention.apply(m, Map.of());

        var utxo = pruned.table("address-utxo");
        assertThat(utxo.files()).extracting(SnapshotManifest.FileEntry::path).allMatch(p -> p.contains("/pruned_"));
        assertThat(utxo.rowCount()).isEqualTo(20);
        assertThat(utxo.bounds()).containsEntry("slot.min", "40");
        assertThat(utxo.historySplit()).isNull();
        assertThat(pruned.retention()).containsEntry(Retention.UTXO_SPENT_SAFE_BLOCKS, "2160");
        assertThat(m.historySplits()).containsExactly(Map.entry(Retention.UTXO_SPENT_SAFE_BLOCKS, "2160"));

        var whole = full.table("address-utxo");
        assertThat(whole.files()).extracting(SnapshotManifest.FileEntry::path).allMatch(p -> p.contains("/ducklake-"));
        assertThat(whole.rowCount()).isEqualTo(200);
        assertThat(whole.bounds()).containsEntry("slot.min", "0");
        assertThat(full.retention()).isNull();

        assertThatThrownBy(() -> Retention.apply(m, Map.of(Retention.UTXO_SPENT_SAFE_BLOCKS, "100")))
                .hasMessageContaining("no history split");
        assertThatThrownBy(() -> Retention.apply(manifest(table("block", List.of(), 0)),
                Map.of(Retention.TX_INPUT_SAFE_BLOCKS, "2160"))).hasMessageContaining("no history split");
    }

    @Test
    void theTargetsPruningSettingsChooseTheCurrentTier() {
        var m = manifest(splitUtxo());
        Map<String, String> notes = new java.util.TreeMap<>();

        assertThat(Retention.forTarget(m, Map.of(), Map.of(), notes)).isEmpty();
        assertThat(notes.get("address_utxo")).contains("full history").contains("store.utxo.pruning-enabled is off");

        assertThat(Retention.forTarget(m, Map.of("store.utxo.pruning-enabled", "true"), Map.of(), notes))
                .containsExactly(Map.entry(Retention.UTXO_SPENT_SAFE_BLOCKS, "2160"));
        assertThat(notes.get("address_utxo")).startsWith("address_utxo: current rows only");

        assertThat(Retention.forTarget(m, Map.of("store.utxo.pruning-enabled", "true",
                "store.utxo.pruning-safe-blocks", "1000"), Map.of(), notes)).containsKey(Retention.UTXO_SPENT_SAFE_BLOCKS);
        assertThat(Retention.forTarget(m, Map.of("store.utxo.pruning-enabled", "true",
                "store.utxo.pruning-safe-blocks", "5000"), Map.of("epoch_stake.epochs", "8"), notes))
                .containsExactly(Map.entry("epoch_stake.epochs", "8"));
        assertThat(notes.get("address_utxo")).contains("wider than the snapshot's 2160");
    }
}
