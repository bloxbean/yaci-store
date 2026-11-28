package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
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
import java.sql.Statement;
import java.util.Properties;

/**
 * Stops a node from silently starting a second, empty DuckLake catalog after the default catalog
 * type changed from {@code postgresql} (3.0.0 betas) to {@code duckdb}.
 *
 * <p>The check fires only when all of these hold:
 * <ul>
 *   <li>{@code yaci.store.analytics.ducklake.catalog-type} is not set, so the default applies;</li>
 *   <li>the DuckDB catalog file does not exist yet, so this would be its first start on it;</li>
 *   <li>the PostgreSQL database the old default used (the catalog URL, or the main datasource)
 *       already holds a DuckLake catalog.</li>
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
        this(environment, properties, CatalogTypeGuard::queryDuckLakeCatalog);
    }

    CatalogTypeGuard(Environment environment, AnalyticsStoreProperties properties, CatalogProbe probe) {
        this.environment = environment;
        this.properties = properties;
        this.probe = probe;
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

    /** DuckLake keeps its PostgreSQL catalog tables in the {@code public} schema. */
    static boolean queryDuckLakeCatalog(String url, String username, String password) throws SQLException {
        Properties info = new Properties();
        if (username != null) info.setProperty("user", username);
        if (password != null) info.setProperty("password", password);
        // Fail fast on an unreachable host instead of waiting for the OS TCP timeout (seconds).
        info.setProperty("connectTimeout", "10");
        info.setProperty("socketTimeout", "30");
        try (Connection conn = DriverManager.getConnection(url, info);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT to_regclass('public.ducklake_snapshot') IS NOT NULL")) {
            return rs.next() && rs.getBoolean(1);
        }
    }
}
