package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StakeAddressBalanceSnapshotExporterTest {
    @Test
    void registersWithAnalyticsWithoutAnExtraOptIn() {
        var context = new ApplicationContextRunner()
                .withUserConfiguration(StakeAddressBalanceSnapshotExporter.class)
                .withBean(StorageWriter.class, () -> mock(StorageWriter.class))
                .withBean(ExportStateService.class, () -> mock(ExportStateService.class))
                .withBean(EraService.class, () -> mock(EraService.class))
                .withBean(AnalyticsStoreProperties.class, AnalyticsStoreProperties::new)
                .withBean(AdaPotJobStorage.class, () -> mock(AdaPotJobStorage.class));
        context.withPropertyValues("yaci.store.analytics.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(StakeAddressBalanceSnapshotExporter.class));
        context.run(ctx -> assertThat(ctx).doesNotHaveBean(StakeAddressBalanceSnapshotExporter.class));
        context.withPropertyValues("yaci.store.analytics.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(StakeAddressBalanceSnapshotExporter.class));
    }

    @Test
    void preservesIntraDayHistoryForEpochCutoffsAndRollback() throws Exception {
        var writer = mock(StorageWriter.class);
        when(writer.getSourceSchema()).thenReturn("source");
        var exporter = new StakeAddressBalanceSnapshotExporter(writer, null, null,
                new AnalyticsStoreProperties(), null);
        try (var conn = DriverManager.getConnection("jdbc:duckdb:"); var st = conn.createStatement()) {
            st.execute("CREATE SCHEMA source");
            st.execute("CREATE TABLE source.stake_address_balance(address VARCHAR, quantity BIGINT, "
                    + "block_time BIGINT, block BIGINT, epoch INTEGER, slot BIGINT)");
            st.execute("INSERT INTO source.stake_address_balance VALUES "
                    + "('a',100,1700000000,1,1,10), ('a',200,1700000001,2,1,20),"
                    + "('a',300,1700000002,3,2,30), ('a',400,1700000003,4,2,40)");
            // Execute the export's SQL locally, with the PostgreSQL table-function boundary stubbed.
            st.execute("CREATE MACRO postgres_query(db, sql) AS TABLE SELECT * FROM query(sql)");
            String sql = exporter.buildQuery(PartitionValue.ofDate(LocalDate.of(2023, 11, 14)),
                    new SlotRange(10, 40));
            List<Long> slots = new ArrayList<>();
            try (var rs = st.executeQuery(sql)) {
                while (rs.next()) slots.add(rs.getLong("slot"));
            }
            assertThat(slots).containsExactly(10L, 20L, 30L);
            try (var rs = st.executeQuery("SELECT quantity FROM (" + sql + ") WHERE slot <= 25 ORDER BY slot DESC LIMIT 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(200);
            }
            try (var rs = st.executeQuery("SELECT quantity FROM (" + sql + ") WHERE slot <= 15 ORDER BY slot DESC LIMIT 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(100);
            }
        }
        assertThat(exporter.getTableName()).isEqualTo("stake_address_balance_snapshot");
        assertThat(exporter.getFederationBoundaryColumn()).isNull();
    }
}
