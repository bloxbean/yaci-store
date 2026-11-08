package com.bloxbean.cardano.yaci.store.core.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Refuses to start on a schema whose snapshot import has not finished.
 *
 * <p>While {@code snapshot import} runs, the target schema holds partly loaded tables with their keys
 * and indexes dropped, tracked in the import journal table {@value #RUN_TABLE}. The journal is dropped
 * once {@code snapshot validate --mark-ready} passes, so a journal row with any status other than
 * READY means the schema must not be migrated or synced yet.
 *
 * <p>The check runs as soon as the primary DataSource is created, i.e. before Flyway, JPA and the
 * sync start. Only PostgreSQL is checked, as the importer supports no other database.
 */
@Component
@Slf4j
public class SnapshotImportGuard implements BeanPostProcessor {
    // Owned by components/snapshot (ImportJournal); duplicated here so core does not depend on it.
    static final String RUN_TABLE = "_yaci_snapshot_import_run";
    static final String STATUS_READY = "READY";

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource && "dataSource".equals(beanName)) {
            check(dataSource);
        }
        return bean;
    }

    static void check(DataSource dataSource) {
        Optional<String> error;
        try (Connection conn = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(conn.getMetaData().getDatabaseProductName()))
                return;
            error = findIncompleteImport(conn, conn.getSchema());
        } catch (SQLException e) {
            // Not this check's job to report an unreachable database; the next database user will.
            log.warn("Could not check the schema for an unfinished snapshot import: {}", e.getMessage());
            return;
        }

        error.ifPresent(message -> {
            log.error(message);
            throw new IllegalStateException(message);
        });
    }

    /**
     * @return an error message when {@code schema} holds a snapshot import that is not READY
     */
    static Optional<String> findIncompleteImport(Connection conn, String schema) throws SQLException {
        if (schema == null)
            return Optional.empty();

        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM information_schema.tables"
                + " WHERE table_schema = ? AND table_name = ?")) {
            ps.setString(1, schema);
            ps.setString(2, RUN_TABLE);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return Optional.empty();
            }
        }

        String sql = "SELECT snapshot_id, status FROM \"" + schema.replace("\"", "\"\"") + "\".\"" + RUN_TABLE + "\""
                + " WHERE status <> '" + STATUS_READY + "' ORDER BY started_at DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next())
                return Optional.empty();

            String snapshotId = rs.getString(1);
            String status = rs.getString(2);
            String nextStep = "LOADING".equals(status)
                    ? "Resume it with 'snapshot import' using the same manifest, then run 'snapshot validate --mark-ready'."
                    : "Run 'snapshot validate --mark-ready' to validate it, or resume 'snapshot import' if it did not finish loading.";
            return Optional.of(String.format("Schema '%s' holds an unfinished snapshot import: snapshot %s is %s "
                    + "(journal table %s). Tables may be partly loaded and keys or indexes may still be dropped, "
                    + "so yaci-store will not start on it. %s Check progress with 'snapshot import-status'.",
                    schema, snapshotId, status, RUN_TABLE, nextStep));
        }
    }
}
