package com.bloxbean.cardano.yaci.store.analytics.helper;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.writer.DuckLakeWriterService;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeWriterLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PostgreSQL DuckLake catalog against a real PostgreSQL: one catalog per instance in a shared
 * database, the pre-existing catalog in {@code public} kept, a second instance refused the first
 * one's catalog, and a least-privilege user whose password needs quoting.
 *
 * <p>Runs only when {@code YACI_ANALYTICS_TEST_PG_HOST}, {@code _PORT}, {@code _DB},
 * {@code _USER} and {@code _PASSWORD} point at a throwaway PostgreSQL whose user may create
 * schemas and roles, e.g.
 * {@code docker run -d -e POSTGRES_PASSWORD=pw -p 127.0.0.1:47731:5432 postgres:16-alpine}.</p>
 */
@EnabledIfEnvironmentVariable(named = "YACI_ANALYTICS_TEST_PG_HOST", matches = ".+")
class PostgresCatalogSchemaIT {

    private static final String HOST = System.getenv("YACI_ANALYTICS_TEST_PG_HOST");
    private static final String PORT = System.getenv().getOrDefault("YACI_ANALYTICS_TEST_PG_PORT", "5432");
    private static final String DB = System.getenv().getOrDefault("YACI_ANALYTICS_TEST_PG_DB", "postgres");
    private static final String USER = System.getenv().getOrDefault("YACI_ANALYTICS_TEST_PG_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("YACI_ANALYTICS_TEST_PG_PASSWORD", "");

    @TempDir
    Path tempDir;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8).replace("-", "");
    private final List<String> schemas = new ArrayList<>();
    private final List<String> roles = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        admin("SELECT 1");
    }

    @AfterEach
    void cleanUp() throws Exception {
        for (String schema : schemas) {
            admin("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
        }
        for (String role : roles) {
            admin("DROP OWNED BY \"" + role + "\"");
            admin("DROP ROLE IF EXISTS \"" + role + "\"");
        }
    }

    @Test
    void twoInstancesInOneDatabaseGetTheirOwnCatalogs() throws Exception {
        String preprod = schema("preprod");
        String mainnet = schema("mainnet");

        try (Connection a = DriverManager.getConnection("jdbc:duckdb:");
             Connection b = DriverManager.getConnection("jdbc:duckdb:")) {
            helper(preprod, null, "a", USER, PASSWORD).prepareConnectionForDuckLake(a, false, false);
            helper(mainnet, null, "b", USER, PASSWORD).prepareConnectionForDuckLake(b, false, false);
            exec(a, "CREATE TABLE t AS SELECT 1 AS v");
            exec(b, "CREATE TABLE t AS SELECT 2 AS v");
            assertEquals(1, single(a, "SELECT v FROM t"));
            assertEquals(2, single(b, "SELECT v FROM t"));
        }
        assertTrue(hasCatalog(preprod));
        assertTrue(hasCatalog(mainnet));
        assertEquals(DB + "/" + preprod, owner(preprod));
        assertEquals(DB + "/" + mainnet, owner(mainnet));
    }

    @Test
    void theWriterExportsAndReplacesPartitionsInACatalogOutsidePublic() throws Exception {
        // The writer reads DuckLake's metadata tables (partitioning, file statistics) on every
        // export after the first; they live in the catalog schema, not in public
        String preprod = schema("preprod");
        AnalyticsStoreProperties properties = properties(preprod, null, "w");
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(preprod, USER, PASSWORD), properties);
        try (com.zaxxer.hikari.HikariDataSource pool = new com.zaxxer.hikari.HikariDataSource()) {
            pool.setJdbcUrl("jdbc:duckdb:");
            pool.setDriverClassName("org.duckdb.DuckDBDriver");
            pool.setMaximumPoolSize(1);
            DuckLakeWriterService writer = new DuckLakeWriterService(pool, properties, helper,
                    new DuckLakeWriterLock(java.time.Duration.ofSeconds(30)));
            String march1 = Path.of(properties.getExportPath(), "block", "date=2024-03-01", "data.parquet").toString();
            String march2 = Path.of(properties.getExportPath(), "block", "date=2024-03-02", "data.parquet").toString();
            writer.export(blocks(1709251200L, 5), march1, "block_time");
            writer.export(blocks(1709337600L, 3), march2, "block_time");
            writer.export(blocks(1709251200L, 4), march1, "block_time");   // replace
            try (Connection c = pool.getConnection()) {
                // The second and third export read the partitioning from the metadata tables
                assertEquals(7, single(c, "SELECT count(*) FROM ducklake_catalog.block"));
            }
        }
        assertTrue(hasCatalog(preprod));
    }

    private static String blocks(long firstTime, int count) {
        return "SELECT range AS slot, to_timestamp(" + firstTime + " + range) AS block_time FROM range(" + count + ")";
    }

    @Test
    void existingPublicCatalogIsKeptAndNotSharedWithASecondInstance() throws Exception {
        // A catalog created the old way: always in public, without METADATA_SCHEMA
        String legacy = "public";
        assertFalse(hasCatalog(legacy), "test needs a database without a catalog in public");
        String preprod = schema("preprod");
        String mainnet = schema("mainnet");
        Path dataA = tempDir.resolve("a");
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            exec(c, "LOAD ducklake");
            exec(c, "ATTACH 'ducklake:postgres:" + libpq(USER, PASSWORD).replace("'", "''")
                    + " options=''-csearch_path=public''' AS old (DATA_PATH '" + dataA + "')");
            exec(c, "CREATE TABLE old.t AS SELECT 7 AS v");
        }
        schemasToDropInPublic = true;
        try {
            try (Connection a = DriverManager.getConnection("jdbc:duckdb:")) {
                helper(preprod, null, "a", USER, PASSWORD).prepareConnectionForDuckLake(a, false, false);
                assertEquals(7, single(a, "SELECT v FROM t"), "the existing catalog in public is used");
            }
            assertFalse(hasCatalog(preprod), "no new empty catalog is created");
            assertTrue(owner(legacy).endsWith("/" + preprod), owner(legacy));

            try (Connection b = DriverManager.getConnection("jdbc:duckdb:")) {
                SQLException e = assertThrows(SQLException.class,
                        () -> helper(mainnet, null, "b", USER, PASSWORD).prepareConnectionForDuckLake(b, false, false));
                assertTrue(e.getMessage().contains("belongs to another"), e.getMessage());
            }
            try (Connection b = DriverManager.getConnection("jdbc:duckdb:")) {
                // ... and works with a schema of its own
                helper(mainnet, mainnet, "b", USER, PASSWORD).prepareConnectionForDuckLake(b, false, false);
                exec(b, "CREATE TABLE t AS SELECT 8 AS v");
                assertEquals(8, single(b, "SELECT v FROM t"));
            }
        } finally {
            dropDuckLakeTablesInPublic();
        }
    }

    @Test
    void leastPrivilegeUserWithAHardPasswordCanUseItsOwnSchema() throws Exception {
        String role = "lim_" + suffix;
        String password = "p w'd\"x";
        String schema = schema("preprod");
        roles.add(role);
        admin("CREATE ROLE \"" + role + "\" LOGIN PASSWORD '" + password.replace("'", "''") + "'");
        admin("GRANT CONNECT ON DATABASE \"" + DB + "\" TO \"" + role + "\"");
        admin("GRANT USAGE, CREATE ON SCHEMA \"" + schema + "\" TO \"" + role + "\"");
        admin("REVOKE CREATE ON SCHEMA public FROM PUBLIC");

        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            helper(schema, null, "a", role, password).prepareConnectionForDuckLake(c, false, false);
            exec(c, "CREATE TABLE t AS SELECT 3 AS v");
            assertEquals(3, single(c, "SELECT v FROM t"));
        }
        assertTrue(hasCatalog(schema));
    }

    // ---------------------------------------------------------------------

    private boolean schemasToDropInPublic;

    private DuckDbConnectionHelper helper(String sourceSchema, String catalogSchema, String dataDir,
                                          String user, String password) {
        return new DuckDbConnectionHelper(environment(sourceSchema, user, password),
                properties(sourceSchema, catalogSchema, dataDir));
    }

    private static Environment environment(String sourceSchema, String user, String password) {
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.datasource.url"))
                .thenReturn("jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB + "?currentSchema=" + sourceSchema);
        when(env.getProperty("spring.datasource.username")).thenReturn(user);
        when(env.getProperty("spring.datasource.password")).thenReturn(password);
        return env;
    }

    private AnalyticsStoreProperties properties(String sourceSchema, String catalogSchema, String dataDir) {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(tempDir.resolve(dataDir).toString());
        properties.getStorage().setType("ducklake");
        properties.getDucklake().setCatalogType("postgresql");
        properties.getDucklake().setCatalogSchema(catalogSchema);
        return properties;
    }

    private String schema(String name) throws Exception {
        String schema = name + "_" + suffix;
        schemas.add(schema);
        admin("CREATE SCHEMA \"" + schema + "\"");
        return schema;
    }

    private boolean hasCatalog(String schema) throws Exception {
        return "true".equals(adminQuery("SELECT (to_regclass('\"" + schema + "\".ducklake_metadata') IS NOT NULL)::text"));
    }

    private String owner(String schema) throws Exception {
        return adminQuery("SELECT value FROM \"" + schema + "\".ducklake_metadata WHERE key = '"
                + CatalogSchemas.OWNER_KEY + "'");
    }

    private void dropDuckLakeTablesInPublic() throws Exception {
        if (!schemasToDropInPublic) {
            return;
        }
        admin("DO $$ DECLARE r record; BEGIN FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' "
                + "AND tablename LIKE 'ducklake%' LOOP EXECUTE 'DROP TABLE public.' || quote_ident(r.tablename) "
                + "|| ' CASCADE'; END LOOP; END $$");
    }

    private static String libpq(String user, String password) {
        return "dbname='" + DB + "' host='" + HOST + "' port='" + PORT + "' user='" + user + "' password='"
                + password.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static void admin(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            exec(c, "LOAD postgres_scanner");
            exec(c, "ATTACH '" + libpq(USER, PASSWORD).replace("'", "''") + "' AS adm (TYPE POSTGRES)");
            exec(c, "CALL postgres_execute('adm', '" + sql.replace("'", "''") + "')");
        }
    }

    private static String adminQuery(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            exec(c, "LOAD postgres_scanner");
            exec(c, "ATTACH '" + libpq(USER, PASSWORD).replace("'", "''") + "' AS adm (TYPE POSTGRES)");
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM postgres_query('adm', '" + sql.replace("'", "''") + "')")) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private static long single(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
