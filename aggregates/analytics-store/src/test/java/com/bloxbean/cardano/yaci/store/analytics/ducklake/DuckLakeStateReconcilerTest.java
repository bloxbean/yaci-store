package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.DuckLakeWriterService;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuckLakeStateReconcilerTest {

    // 2024-03-01T00:00:00Z
    private static final long MARCH_1 = 1709251200L;

    @TempDir
    Path tempDir;

    private DuckLakeTestCatalog catalog;
    private ExportStateService stateService;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(DuckLakeTestCatalog.duckLakeAvailable(), "DuckLake extension not available");
        catalog = new DuckLakeTestCatalog(tempDir);
        stateService = mock(ExportStateService.class);
        when(stateService.resetCompletedPartitions(anyString(), anyCollection(), anyString()))
                .thenAnswer(inv -> ((java.util.Collection<?>) inv.getArgument(1)).size());
    }

    @AfterEach
    void tearDown() {
        if (catalog != null) {
            catalog.close();
        }
    }

    @Test
    void resetsCompletedPartitionsTheCatalogHasNoRowsFor() throws Exception {
        DuckLakeWriterService writer = catalog.writer();
        writer.export(blocks(0, 5, MARCH_1), catalog.path("block", "date=2024-03-01"), "block_time");
        writer.export("SELECT 450 AS epoch, range AS amount FROM range(2)", catalog.path("reward", "epoch=450"),
                "block_time");

        when(stateService.getCompletedPartitionsWithRows()).thenReturn(Map.of(
                "block", Map.of("date=2024-03-01", 5L, "date=2024-03-02", 7L),
                "reward", Map.of("epoch=450", 2L, "epoch=451", 4L),
                "transaction", Map.of("date=2024-03-01", 9L)));

        DuckLakeStateReconciler.Result result = reconciler(catalog.helper).reconcile();

        assertThat(result.reset()).containsOnlyKeys("block", "reward", "transaction");
        assertThat(result.reset().get("block")).containsExactly("date=2024-03-02");
        assertThat(result.reset().get("reward")).containsExactly("epoch=451");
        assertThat(result.reset().get("transaction")).containsExactly("date=2024-03-01");
        verify(stateService).resetCompletedPartitions(eq("block"), eq(List.of("date=2024-03-02")),
                eq(DuckLakeStateReconciler.RESET_REASON));
        verify(stateService, never()).resetCompletedPartitions(eq("block"), eq(List.of("date=2024-03-01")),
                anyString());
    }

    @Test
    void anEmptyCatalogResetsEveryCompletedPartition() throws Exception {
        when(stateService.getCompletedPartitionsWithRows()).thenReturn(Map.of(
                "block", Map.of("date=2024-03-01", 5L)));

        DuckLakeStateReconciler.Result result = reconciler(catalog.helper).reconcile();

        assertThat(result.resetCount()).isEqualTo(1);
        verify(stateService).resetCompletedPartitions(eq("block"), eq(List.of("date=2024-03-01")), anyString());
    }

    @Test
    void anUnreachableCatalogChangesNoState() throws Exception {
        when(stateService.getCompletedPartitionsWithRows()).thenReturn(Map.of(
                "block", Map.of("date=2024-03-01", 5L)));
        DuckDbConnectionHelper unreachable = spy(catalog.helper);
        doThrow(new SQLException("Could not connect to the catalog"))
                .when(unreachable).prepareConnectionForDuckLake(any(Connection.class), eq(false), eq(false));

        DuckLakeStateReconciler reconciler = reconciler(unreachable);
        assertThatThrownBy(reconciler::reconcile).isInstanceOf(SQLException.class);
        reconciler.reconcileOnStartup();

        verify(stateService, never()).resetCompletedPartitions(anyString(), anyCollection(), anyString());
    }

    @Test
    void partitionsWithFewerRowsAreOnlyReported() throws Exception {
        catalog.writer().export(blocks(0, 5, MARCH_1), catalog.path("block", "date=2024-03-01"), "block_time");
        when(stateService.getCompletedPartitionsWithRows()).thenReturn(Map.of(
                "block", Map.of("date=2024-03-01", 6L)));

        DuckLakeStateReconciler.Result result = reconciler(catalog.helper).reconcile();

        assertThat(result.reset()).isEmpty();
        assertThat(result.shortPartitions()).containsEntry("block", List.of("date=2024-03-01"));
        verify(stateService, never()).resetCompletedPartitions(anyString(), anyCollection(), anyString());
    }

    @Test
    void disabledReconciliationDoesNothing() {
        catalog.properties.getDucklake().setReconcileStateOnStartup(false);

        reconciler(catalog.helper).reconcileOnStartup();

        verify(stateService, never()).getCompletedPartitionsWithRows();
    }

    @Test
    void resetsEmptyCompletedPartitionsOnlyWhereTheTableAlwaysHasRows() {
        TableExporter epochTable = mock(TableExporter.class);
        when(epochTable.allowsEmptyPartition(PartitionValue.ofEpoch(400))).thenReturn(false);
        when(epochTable.allowsEmptyPartition(PartitionValue.ofEpoch(10))).thenReturn(true);
        TableExporter withdrawals = mock(TableExporter.class);
        when(withdrawals.allowsEmptyPartition(any())).thenReturn(true);
        TableExporterRegistry registry = mock(TableExporterRegistry.class);
        when(registry.getExporter("epoch")).thenReturn(epochTable);
        when(registry.getExporter("withdrawal")).thenReturn(withdrawals);
        when(registry.getExporter("dropped_table")).thenThrow(new IllegalArgumentException("unknown"));
        when(stateService.getCompletedEmptyPartitions()).thenReturn(Map.of(
                "epoch", java.util.List.of("epoch=400", "epoch=10"),
                "withdrawal", java.util.List.of("date=2024-03-01"),
                "dropped_table", java.util.List.of("date=2024-03-01")));

        DuckLakeStateReconciler reconciler = reconciler(catalog.helper);
        @SuppressWarnings("unchecked")
        org.springframework.beans.factory.ObjectProvider<TableExporterRegistry> provider =
                mock(org.springframework.beans.factory.ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        reconciler.setExporterRegistry(provider);

        assertThat(reconciler.resetImpossibleEmptyPartitions())
                .containsExactly(Map.entry("epoch", java.util.List.of("epoch=400")));
        verify(stateService).resetCompletedPartitions(eq("epoch"), eq(java.util.List.of("epoch=400")), anyString());
        verify(stateService, never()).resetCompletedPartitions(eq("withdrawal"), anyCollection(), anyString());
    }

    private DuckLakeStateReconciler reconciler(DuckDbConnectionHelper helper) {
        return new DuckLakeStateReconciler(catalog.dataSource, helper, catalog.lock, stateService, catalog.properties);
    }

    private static String blocks(long firstSlot, int count, long firstTime) {
        return "SELECT " + firstSlot + " + range AS slot, to_timestamp(" + firstTime + " + range) AS block_time "
                + "FROM range(" + count + ")";
    }
}
