package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.export.InspectionReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BlockerReportTest {

    @TempDir
    Path work;

    @Test
    void writesEveryPartitionOfATruncatedListAndNothingOtherwise() throws Exception {
        List<String> many = IntStream.rangeClosed(1, 12).mapToObj(e -> "epoch=" + e + " has no completed export record").toList();
        var blocker = "[reward] incomplete export: epoch=1 ... (12 partitions affected, 7 not shown).";
        Path file = SnapshotCommands.writeBlockerReport(report(List.of(blocker),
                Map.of("reward", many, "block", List.of("date=2026-01-02 has no completed export record"))), work);

        assertThat(file).isEqualTo(work.resolve("snapshot-blockers.txt"));
        List<String> lines = Files.readAllLines(file);
        assertThat(lines.get(0)).isEqualTo(blocker);
        assertThat(lines).contains("[reward] 12 incomplete partition(s):").containsAll(many)
                .noneMatch(l -> l.startsWith("[block]"));

        assertThat(SnapshotCommands.writeBlockerReport(report(List.of("other"),
                Map.of("block", many.subList(0, 5))), work.resolve("none"))).isNull();
    }

    private static InspectionReport report(List<String> blockers, Map<String, List<String>> partitions) {
        return new InspectionReport(1, "1.4.0", Map.of(), 10, null, Map.of(), List.of(), 0, List.of(), List.of(),
                List.of(), blockers, List.of(), partitions);
    }
}
