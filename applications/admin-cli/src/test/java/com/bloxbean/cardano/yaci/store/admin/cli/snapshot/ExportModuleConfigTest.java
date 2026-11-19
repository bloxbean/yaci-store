package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** An export run without the source's profile must not silently drop the ledger-state tables. */
class ExportModuleConfigTest {
    private final SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();

    @Test
    void disabledModuleWithExportedRowsIsAMismatch() {
        String problem = SnapshotCliSupport.disabledModulesWithExportedRows(registry,
                Map.of("adapot", false, "blocks", true),
                Map.of("adapot", Map.of("epoch=208", 1L), "block", Map.of("date=2017-09-23", 21L)));
        assertThat(problem).contains("adapot (store.adapot.enabled=false): adapot")
                .contains("-Dspring.profiles.active");
    }

    @Test
    void disabledModuleThatTheSourceNeverExportedIsFine() {
        assertThat(SnapshotCliSupport.disabledModulesWithExportedRows(registry,
                Map.of("adapot", false), Map.of("adapot", Map.of("epoch=208", 0L)))).isNull();
        assertThat(SnapshotCliSupport.disabledModulesWithExportedRows(registry,
                Map.of("adapot", false), Map.of())).isNull();
    }

    @Test
    void enabledModulesAreNotReported() {
        assertThat(SnapshotCliSupport.disabledModulesWithExportedRows(registry,
                Map.of("adapot", true), Map.of("adapot", Map.of("epoch=208", 1L)))).isNull();
    }
}
