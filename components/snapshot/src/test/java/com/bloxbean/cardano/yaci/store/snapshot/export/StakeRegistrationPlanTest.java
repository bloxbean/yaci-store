package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StakeRegistrationPlanTest {
    @Test
    void rejectsLegacyCatalogWithoutDepositBeforePackaging() throws Exception {
        var catalog = mock(DuckLakeCatalog.class);
        var columns = new HashMap<>(Map.of("slot", "BIGINT", "epoch", "INTEGER",
                "block_time", "TIMESTAMPTZ", "date", "DATE"));
        when(catalog.columns("stake_registration", 1)).thenReturn(columns);
        var spec = SnapshotSpecRegistry.builtIn().byId("stake-registration").orElseThrow();
        var planner = new ExportPlanner(catalog);
        assertThat(planner.plan(spec, 1, 300, 100).problems())
                .anyMatch(p -> p.contains("deposit") && p.contains("not exported"));
        columns.put("deposit", "BIGINT");
        assertThat(planner.plan(spec, 1, 300, 100).problems()).isEmpty();
    }
}
