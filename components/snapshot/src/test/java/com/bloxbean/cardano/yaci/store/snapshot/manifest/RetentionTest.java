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
}
