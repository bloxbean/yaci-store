package com.bloxbean.cardano.yaci.store.core.configuration;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SnapshotImportGuardTest {

    private JdbcDataSource dataSource;
    private Connection keepAlive;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:snapshot_guard_" + System.nanoTime());
        keepAlive = dataSource.getConnection();
    }

    @AfterEach
    void tearDown() throws SQLException {
        keepAlive.close();
    }

    @Test
    void noJournal_nothingReported() throws SQLException {
        assertThat(SnapshotImportGuard.findIncompleteImport(keepAlive, keepAlive.getSchema())).isEmpty();
        assertThatCode(() -> SnapshotImportGuard.check(asPostgres(dataSource))).doesNotThrowAnyException();
    }

    @Test
    void loadingImport_reportsSnapshotStatusAndResume() throws SQLException {
        journal("snap-1", "LOADING");

        assertThat(SnapshotImportGuard.findIncompleteImport(keepAlive, keepAlive.getSchema()))
                .hasValueSatisfying(message -> assertThat(message)
                        .contains("snap-1", "LOADING", "'snapshot import'", "snapshot validate --mark-ready"));
    }

    @Test
    void validatingImport_reportsMarkReady() throws SQLException {
        journal("snap-2", "VALIDATING");

        assertThat(SnapshotImportGuard.findIncompleteImport(keepAlive, keepAlive.getSchema()))
                .hasValueSatisfying(message -> assertThat(message)
                        .contains("snap-2", "VALIDATING", "snapshot validate --mark-ready"));
    }

    @Test
    void readyImport_nothingReported() throws SQLException {
        journal("snap-3", "READY");

        assertThat(SnapshotImportGuard.findIncompleteImport(keepAlive, keepAlive.getSchema())).isEmpty();
    }

    @Test
    void postgres_unfinishedImport_refusesToStart() throws SQLException {
        journal("snap-4", "LOADING");

        assertThatThrownBy(() -> SnapshotImportGuard.check(asPostgres(dataSource)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("snap-4")
                .hasMessageContaining("LOADING");
    }

    @Test
    void otherDatabase_notChecked() throws SQLException {
        journal("snap-5", "LOADING");

        assertThatCode(() -> SnapshotImportGuard.check(dataSource)).doesNotThrowAnyException();
    }

    @Test
    void onlyThePrimaryDataSourceBeanIsChecked() throws SQLException {
        journal("snap-6", "LOADING");
        DataSource postgres = asPostgres(dataSource);
        SnapshotImportGuard guard = new SnapshotImportGuard();

        assertThat(guard.postProcessAfterInitialization(postgres, "duckDbDataSource")).isSameAs(postgres);
        assertThatThrownBy(() -> guard.postProcessAfterInitialization(postgres, "dataSource"))
                .isInstanceOf(IllegalStateException.class);
    }

    private void journal(String snapshotId, String status) throws SQLException {
        try (Statement st = keepAlive.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS \"_yaci_snapshot_import_run\" ("
                    + " snapshot_id varchar(64) PRIMARY KEY, status varchar(20) NOT NULL,"
                    + " started_at timestamp NOT NULL DEFAULT now())");
            st.execute("INSERT INTO \"_yaci_snapshot_import_run\" (snapshot_id, status)"
                    + " VALUES ('" + snapshotId + "', '" + status + "')");
        }
    }

    /** The H2 database, reporting itself as PostgreSQL. */
    private static DataSource asPostgres(DataSource h2) throws SQLException {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenAnswer(inv -> {
            Connection conn = spy(h2.getConnection());
            DatabaseMetaData md = mock(DatabaseMetaData.class);
            when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
            doReturn(md).when(conn).getMetaData();
            return conn;
        });
        return ds;
    }
}
