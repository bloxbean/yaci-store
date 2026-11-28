package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.CatalogSchemas;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.bloxbean.cardano.yaci.store.analytics.helper.PostgresJdbcUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Stops a node from silently starting a second, empty DuckLake catalog after the default catalog
 * type changed from {@code postgresql} (3.0.0 betas) to {@code duckdb}.
 *
 * <p>The check fires only when all of these hold:
 * <ul>
 *   <li>{@code yaci.store.analytics.ducklake.catalog-type} is not set, so the default applies;</li>
 *   <li>the DuckDB catalog file does not exist yet, so this would be its first start on it;</li>
 *   <li>the PostgreSQL database the old default used (the catalog URL, or the main datasource)
 *       already holds a DuckLake catalog of this node's, in a schema it would have used
 *       ({@link CatalogSchemas}): {@code catalog-schema}, the connection's {@code currentSchema},
 *       or {@code public}. A catalog that records another source, or that records none and was
 *       written for another export path, belongs to another instance and does not count.</li>
 * </ul>
 * A warning would not do: this very start creates the DuckDB file, so the next start would pass
 * the check while exports continue into the empty catalog. For the same reason a database that
 * cannot be checked also refuses the start: the next start, with the database back, checks again.
 */
@Slf4j
public class CatalogTypeGuard {

    static final String PROPERTY = "yaci.store.analytics.ducklake.catalog-type";

    /** Whether a PostgreSQL database already holds a DuckLake catalog. */
    @FunctionalInterface
    interface CatalogProbe {
        boolean hasDuckLakeCatalog(String jdbcUrl, String username, String password) throws SQLException;
    }

    private final Environment environment;
    private final AnalyticsStoreProperties properties;
    private final CatalogProbe probe;

    public CatalogTypeGuard(Environment environment, AnalyticsStoreProperties properties) {
        this(environment, properties, null);
    }

    CatalogTypeGuard(Environment environment, AnalyticsStoreProperties properties, CatalogProbe probe) {
        this.environment = environment;
        this.properties = properties;
        this.probe = probe != null ? probe : this::queryDuckLakeCatalog;
    }

    /**
     * @throws IllegalStateException when the node relies on the default but its catalog is in PostgreSQL
     */
    public void check() {
        if (isSet()
                || !"duckdb".equalsIgnoreCase(properties.getDucklake().getCatalogType())) {
            return;
        }
        String catalogPath = properties.getDucklake().getCatalogPath();
        if (catalogPath != null && Files.exists(Path.of(catalogPath))) {
            return;
        }

        AnalyticsStoreProperties.DuckLake ducklake = properties.getDucklake();
        String url = ducklake.getCatalogUrl() != null ? ducklake.getCatalogUrl()
                : environment.getProperty("spring.datasource.url");
        String urlProperty = ducklake.getCatalogUrl() != null
                ? "yaci.store.analytics.ducklake.catalog-url" : "spring.datasource.url";
        if (url == null || !url.startsWith("jdbc:postgresql:")) {
            return;
        }
        String username = ducklake.getCatalogUsername() != null ? ducklake.getCatalogUsername()
                : environment.getProperty("spring.datasource.username");
        String password = ducklake.getCatalogPassword() != null ? ducklake.getCatalogPassword()
                : environment.getProperty("spring.datasource.password");

        boolean found;
        try {
            found = probe.hasDuckLakeCatalog(url, username, password);
        } catch (SQLException e) {
            // Not chained: the driver's message could carry connection details, so only a redacted copy is kept.
            throw new IllegalStateException("Could not check whether the database at " + urlProperty
                    + " holds a DuckLake catalog from an earlier version, so the DuckDB catalog at " + catalogPath
                    + " was not created (" + DuckDbConnectionHelper.redactSecrets(e.getMessage()) + "). Start again"
                    + " once the database is reachable, or set " + PROPERTY + "=postgresql to keep a PostgreSQL "
                    + "catalog, or " + PROPERTY + "=duckdb to use a DuckDB one.");
        }
        if (found) {
            throw new IllegalStateException("This node's DuckLake catalog is in PostgreSQL, but "
                    + PROPERTY + " is not set and its default is now duckdb, which would start a new, empty "
                    + "catalog at " + catalogPath + ". Set " + PROPERTY + "=postgresql to keep the existing "
                    + "catalog, or " + PROPERTY + "=duckdb to start a new one.");
        }
    }

    /** Whether the setting is given in any form Spring binds: catalog-type, catalogType, an environment variable. */
    private boolean isSet() {
        if (environment instanceof ConfigurableEnvironment) {
            return Binder.get(environment).bind(PROPERTY, String.class).isBound();
        }
        return environment.containsProperty(PROPERTY);
    }

    /** Connects to the catalog database and looks for a catalog of this node's ({@link #hasOwnCatalog}). */
    private boolean queryDuckLakeCatalog(String url, String username, String password) throws SQLException {
        Properties info = new Properties();
        if (username != null) info.setProperty("user", username);
        if (password != null) info.setProperty("password", password);
        // Fail fast on an unreachable host instead of waiting for the OS TCP timeout (seconds).
        info.setProperty("connectTimeout", "10");
        info.setProperty("socketTimeout", "30");
        try (Connection conn = DriverManager.getConnection(url, info)) {
            return hasOwnCatalog(new JdbcCatalogLookup(conn), candidateSchemas(url), ownerIdentity(),
                    properties.getExportPath());
        }
    }

    /** The schemas a PostgreSQL catalog of this node's could be in: catalog-schema, the URL's currentSchema, public. */
    Set<String> candidateSchemas(String catalogUrl) {
        Set<String> schemas = new LinkedHashSet<>();
        String configured = properties.getDucklake().getCatalogSchema();
        if (configured != null && !configured.isBlank()) {
            schemas.add(configured.trim());
        }
        schemas.add(firstSchema(parameter(catalogUrl, "currentSchema")));
        schemas.add(CatalogSchemas.PUBLIC);
        return schemas;
    }

    /** {@code <database>/<schema>} of the main datasource, as the catalog records its owner. */
    private String ownerIdentity() {
        String url = environment.getProperty("spring.datasource.url");
        String schema = parameter(url, "currentSchema");
        String database;
        try {
            database = PostgresJdbcUrl.parse(url).database();
        } catch (IllegalArgumentException | NullPointerException e) {
            database = null;
        }
        return CatalogSchemas.ownerIdentity(database,
                schema == null || schema.isBlank() ? CatalogSchemas.PUBLIC : schema.trim());
    }

    static boolean hasOwnCatalog(CatalogSchemas.CatalogLookup lookup, Set<String> schemas, String owner,
                                 String exportPath) throws SQLException {
        for (String schema : schemas) {
            if (lookup.hasCatalog(schema) && CatalogSchemas.belongsTo(lookup, schema, owner, exportPath)) {
                return true;
            }
        }
        return false;
    }

    private static String parameter(String url, String name) {
        try {
            return PostgresJdbcUrl.parse(url).parameter(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /** {@code currentSchema} may be a search path ({@code a,b}): the catalog goes into the first. */
    private static String firstSchema(String schema) {
        if (schema == null || schema.isBlank()) {
            return CatalogSchemas.PUBLIC;
        }
        String first = schema.split(",")[0].trim();
        if (first.length() > 1 && first.startsWith("\"") && first.endsWith("\"")) {
            return first.substring(1, first.length() - 1).replace("\"\"", "\"");
        }
        return first.isEmpty() ? CatalogSchemas.PUBLIC : first;
    }

    /** Reads the catalog tables over plain JDBC. */
    private record JdbcCatalogLookup(Connection conn) implements CatalogSchemas.CatalogLookup {
        @Override
        public boolean hasCatalog(String schema) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
                st.setString(1, quote(schema) + ".ducklake_metadata");
                try (ResultSet rs = st.executeQuery()) {
                    return rs.next() && rs.getBoolean(1);
                }
            }
        }

        @Override
        public String metadata(String schema, String key) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT value FROM " + quote(schema) + ".ducklake_metadata WHERE key = ? LIMIT 1")) {
                st.setString(1, key);
                try (ResultSet rs = st.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        }

        private static String quote(String identifier) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
    }
}
