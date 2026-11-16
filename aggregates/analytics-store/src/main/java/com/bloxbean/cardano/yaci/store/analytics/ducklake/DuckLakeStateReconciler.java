package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Startup check that the export state and the DuckLake catalog agree.
 *
 * <p>The export state (PostgreSQL) and the DuckLake catalog are stored separately. If the
 * catalog is deleted (the DuckDB-file catalog is then silently recreated empty), recreated,
 * pointed at another schema, or restored from an older backup, the state still lists
 * partitions as COMPLETED that the catalog no longer has. Gap detection skips COMPLETED
 * partitions, so they would never be exported again, and the query layer would treat them as
 * exported.</p>
 *
 * <p>For every table, this compares the COMPLETED partitions that hold rows with the row counts
 * of the catalog's latest snapshot. A partition the catalog has no rows for is reset to FAILED
 * so the scheduler exports it again. All catalog reads happen first; if any of them fails (the
 * catalog is unreachable, for example) nothing is reset. Partitions with fewer rows in the
 * catalog than in the state are only reported.</p>
 *
 * <p>The check runs under the writer lock, so no export commits between the catalog read and
 * the state reset.</p>
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeStateReconciler {

    static final String RESET_REASON = "Reset at startup: the DuckLake catalog has no rows for this partition";
    static final String EMPTY_RESET_REASON = "Reset at startup: stored with no rows, but a final partition of this "
            + "table always has rows (exported too early by an older version)";

    private final DataSource writerDataSource;
    private final DuckDbConnectionHelper connectionHelper;
    private final DuckLakeWriterLock writerLock;
    private final ExportStateService stateService;
    private final AnalyticsStoreProperties properties;
    /** Looked up lazily: the exporters depend on the writer, which this reconciler shares. */
    private ObjectProvider<TableExporterRegistry> exporterRegistry;

    @Autowired(required = false)
    void setExporterRegistry(ObjectProvider<TableExporterRegistry> exporterRegistry) {
        this.exporterRegistry = exporterRegistry;
    }

    public DuckLakeStateReconciler(@Qualifier("duckDbWriterDataSource") DataSource writerDataSource,
                                   DuckDbConnectionHelper connectionHelper,
                                   DuckLakeWriterLock writerLock,
                                   ExportStateService stateService,
                                   AnalyticsStoreProperties properties) {
        this.writerDataSource = writerDataSource;
        this.connectionHelper = connectionHelper;
        this.writerLock = writerLock;
        this.stateService = stateService;
        this.properties = properties;
    }

    /**
     * Outcome of a reconciliation.
     *
     * @param checkedTables tables with COMPLETED partitions that were compared
     * @param reset         partitions reset, by table
     * @param shortPartitions partitions with fewer catalog rows than exported, by table (not reset)
     */
    public record Result(int checkedTables, Map<String, List<String>> reset, Map<String, List<String>> shortPartitions) {
        public int resetCount() {
            return reset.values().stream().mapToInt(List::size).sum();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void reconcileOnStartup() {
        if (!properties.getDucklake().isReconcileStateOnStartup()) {
            log.info("DuckLake state reconciliation is disabled (yaci.store.analytics.ducklake.reconcile-state-on-startup=false)");
            return;
        }
        try {
            resetImpossibleEmptyPartitions();
        } catch (RuntimeException e) {
            log.warn("Could not check the empty COMPLETED partitions ({}); no export state was changed",
                    DuckDbConnectionHelper.redactSecrets(String.valueOf(e.getMessage())));
        }
        try {
            reconcile();
        } catch (SQLException e) {
            log.warn("Could not read the DuckLake catalog to reconcile the export state ({}); no export state "
                    + "was changed", DuckDbConnectionHelper.redactSecrets(e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("DuckLake state reconciliation failed ({}); no export state was changed",
                    DuckDbConnectionHelper.redactSecrets(String.valueOf(e.getMessage())));
        }
    }

    /**
     * Compares the export state with the catalog and resets COMPLETED partitions that have no
     * rows in the catalog.
     *
     * @throws SQLException if the catalog cannot be read; nothing is reset then
     */
    public Result reconcile() throws SQLException {
        Map<String, Map<String, Long>> completed = stateService.getCompletedPartitionsWithRows();
        if (completed.isEmpty()) {
            return new Result(0, Map.of(), Map.of());
        }

        try (DuckLakeWriterLock.Guard ignored = writerLock.acquire();
             Connection conn = writerDataSource.getConnection()) {
            connectionHelper.prepareConnectionForDuckLake(conn, false, false);

            // Read everything first: a failure here leaves the state untouched
            Set<String> catalogTables = listTables(conn);
            Map<String, List<String>> missing = new TreeMap<>();
            Map<String, List<String>> shortPartitions = new TreeMap<>();
            for (Map.Entry<String, Map<String, Long>> table : completed.entrySet()) {
                Map<String, Long> catalogRows = catalogTables.contains(table.getKey())
                        ? rowsByPartition(conn, table.getKey(), table.getValue().keySet())
                        : Map.of();
                for (Map.Entry<String, Long> partition : table.getValue().entrySet()) {
                    long inCatalog = catalogRows.getOrDefault(partition.getKey(), 0L);
                    if (inCatalog == 0) {
                        missing.computeIfAbsent(table.getKey(), t -> new ArrayList<>()).add(partition.getKey());
                    } else if (inCatalog < partition.getValue()) {
                        shortPartitions.computeIfAbsent(table.getKey(), t -> new ArrayList<>())
                                .add(partition.getKey());
                    }
                }
            }

            if (catalogTables.isEmpty()) {
                log.error("The DuckLake catalog has no tables, but the export state lists {} COMPLETED partitions "
                        + "with rows in {} tables. The catalog was probably deleted, recreated or pointed at another "
                        + "location. These partitions are reset and will be exported again from PostgreSQL. "
                        + "If the catalog was moved by mistake, stop the application and restore it.",
                        completed.values().stream().mapToInt(Map::size).sum(), completed.size());
            }

            for (Map.Entry<String, List<String>> table : shortPartitions.entrySet()) {
                log.warn("DuckLake table '{}' holds fewer rows than were exported for {} partitions (e.g. {}); "
                        + "they are kept as they are", table.getKey(), table.getValue().size(), table.getValue().get(0));
            }

            for (Map.Entry<String, List<String>> table : missing.entrySet()) {
                List<String> partitions = table.getValue();
                int reset = stateService.resetCompletedPartitions(table.getKey(), partitions, RESET_REASON);
                log.error("DuckLake catalog has no rows for {} COMPLETED partitions of table '{}' ({}{}); reset {} of "
                        + "them so they are exported again", partitions.size(), table.getKey(),
                        String.join(", ", partitions.subList(0, Math.min(5, partitions.size()))),
                        partitions.size() > 5 ? ", ..." : "", reset);
            }
            if (missing.isEmpty()) {
                log.info("Export state matches the DuckLake catalog ({} tables checked)", completed.size());
            }
            return new Result(completed.size(), missing, shortPartitions);
        }
    }

    /**
     * Resets COMPLETED partitions stored with no rows whose table always has rows for a final
     * partition (for example the {@code epoch} table while epoch-aggr was off, or an AdaPot table
     * exported before its job). Older versions stored such partitions as COMPLETED, and gap
     * detection never exports a COMPLETED partition again. Uses only the export state, so it does
     * not depend on the catalog.
     *
     * @return the partitions reset, by table
     */
    public Map<String, List<String>> resetImpossibleEmptyPartitions() {
        TableExporterRegistry registry = exporterRegistry == null ? null : exporterRegistry.getIfAvailable();
        if (registry == null) {
            return Map.of();
        }
        Map<String, List<String>> reset = new TreeMap<>();
        for (Map.Entry<String, List<String>> table : stateService.getCompletedEmptyPartitions().entrySet()) {
            TableExporter exporter;
            try {
                exporter = registry.getExporter(table.getKey());
            } catch (IllegalArgumentException e) {
                continue;   // no exporter for this table any more
            }
            List<String> impossible = new ArrayList<>();
            for (String key : table.getValue()) {
                try {
                    if (!exporter.allowsEmptyPartition(PartitionValue.parse(key))) {
                        impossible.add(key);
                    }
                } catch (RuntimeException e) {
                    log.debug("Could not check empty partition {} of {}: {}", key, table.getKey(), e.getMessage());
                }
            }
            if (!impossible.isEmpty()) {
                int n = stateService.resetCompletedPartitions(table.getKey(), impossible, EMPTY_RESET_REASON);
                log.warn("Table '{}' had {} COMPLETED partitions stored with no rows although a final partition always "
                        + "has rows ({}{}); reset {} of them so they are exported again", table.getKey(),
                        impossible.size(), String.join(", ", impossible.subList(0, Math.min(5, impossible.size()))),
                        impossible.size() > 5 ? ", ..." : "", n);
                reset.put(table.getKey(), impossible);
            }
        }
        return reset;
    }

    private static Set<String> listTables(Connection conn) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT table_name FROM duckdb_tables() WHERE database_name = 'ducklake_catalog' "
                        + "AND schema_name = 'main'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    /**
     * Rows per partition value ({@code date=2024-01-15}, {@code epoch=450}) in the catalog's
     * latest snapshot, for the partition columns the state uses for this table.
     */
    private static Map<String, Long> rowsByPartition(Connection conn, String table, Set<String> statePartitions)
            throws SQLException {
        Set<String> keys = new TreeSet<>();
        for (String partition : statePartitions) {
            int eq = partition.indexOf('=');
            if (eq > 0) {
                keys.add(partition.substring(0, eq));
            }
        }
        Set<String> columns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT column_name FROM duckdb_columns() WHERE database_name = 'ducklake_catalog' "
                        + "AND schema_name = 'main' AND table_name = ?")) {
            stmt.setString(1, table);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString(1));
                }
            }
        }

        Map<String, Long> rows = new HashMap<>();
        for (String key : keys) {
            if (!columns.contains(key)) {
                continue; // no such column: no row can belong to these partitions
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(String.format(
                         "SELECT CAST(\"%s\" AS VARCHAR), COUNT(*) FROM ducklake_catalog.main.\"%s\" "
                                 + "WHERE \"%s\" IS NOT NULL GROUP BY 1",
                         key, table.replace("\"", "\"\""), key))) {
                while (rs.next()) {
                    rows.put(key + "=" + rs.getString(1), rs.getLong(2));
                }
            }
        }
        return rows;
    }
}
