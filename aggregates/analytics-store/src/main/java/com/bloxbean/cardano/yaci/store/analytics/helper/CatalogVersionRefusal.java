package com.bloxbean.cardano.yaci.store.analytics.helper;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A DuckLake catalog that this engine refuses to open, with the resync steps an operator
 * needs (ADR 0008 D2).
 *
 * <p>DuckLake on DuckDB 2.x opens 1.0 and 1.1 catalogs as they are. It refuses two kinds of
 * catalog and leaves both untouched:</p>
 * <ul>
 *   <li>an <b>older</b> catalog that needs migrating, e.g. spec 0.3 written by DuckDB 1.4.x
 *       ({@code v3.0.0-beta1}-{@code beta3}): "DuckLake catalog version mismatch: catalog
 *       version is 0.3, but the extension requires version 1.1";</li>
 *   <li>a catalog <b>newer</b> than the engine: "Unsupported ducklake_version '1.2'".</li>
 * </ul>
 */
public class CatalogVersionRefusal extends SQLException {

    private static final Pattern OLDER = Pattern.compile(
            "catalog version mismatch: catalog version is ([^,\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NEWER = Pattern.compile(
            "Unsupported ducklake_version '([^']+)'", Pattern.CASE_INSENSITIVE);

    private final String catalogVersion;
    private final boolean newerThanEngine;

    private CatalogVersionRefusal(String message, String catalogVersion, boolean newerThanEngine,
                                  SQLException cause) {
        super(message, cause.getSQLState(), cause.getErrorCode());
        setStackTrace(cause.getStackTrace());
        this.catalogVersion = catalogVersion;
        this.newerThanEngine = newerThanEngine;
    }

    /** The catalog's DuckLake spec version as reported by the engine. */
    public String getCatalogVersion() {
        return catalogVersion;
    }

    /** {@code true} when the catalog was written by a newer DuckLake than this engine's. */
    public boolean isNewerThanEngine() {
        return newerThanEngine;
    }

    /**
     * Turn an attach failure into a refusal with resync steps, or return {@code null} when the
     * failure is not about the catalog version. The cause chain is not kept: attach errors can
     * echo the catalog connection string.
     */
    public static CatalogVersionRefusal from(SQLException e, String catalogType,
                                             AnalyticsStoreProperties properties) {
        String message = e.getMessage();
        if (message == null) {
            return null;
        }
        Matcher newer = NEWER.matcher(message);
        if (newer.find()) {
            String version = newer.group(1);
            return new CatalogVersionRefusal(
                    "The DuckLake catalog has spec version " + version + ", which is newer than this "
                            + "engine supports. It was written by a newer DuckDB. Run a yaci-store release "
                            + "with that DuckDB version, or resync the analytics store. "
                            + resyncSteps(catalogType, properties),
                    version, true, e);
        }
        Matcher older = OLDER.matcher(message);
        if (older.find()) {
            String version = older.group(1);
            boolean triedMigration = properties.getDucklake().isAutoMigrate();
            String reason = triedMigration
                    ? "Automatic migration was enabled but did not succeed. "
                    : "It was created by an older DuckDB (yaci-store v3.0.0-beta3 or earlier) and needs "
                    + "migrating, which yaci-store does not do implicitly. ";
            return new CatalogVersionRefusal(
                    "The DuckLake catalog has spec version " + version + ", which this engine cannot open "
                            + "without migrating it. " + reason
                            + "Resync the analytics store (supported): " + resyncSteps(catalogType, properties)
                            + (triedMigration ? "" : " Or, unsupported and one-way, set "
                            + "yaci.store.analytics.ducklake.auto-migrate=true to let DuckLake migrate it in "
                            + "place; back up the catalog first."),
                    version, false, e);
        }
        return null;
    }

    static String resyncSteps(String catalogType, AnalyticsStoreProperties properties) {
        String catalog = "postgresql".equalsIgnoreCase(catalogType)
                ? "the ducklake_* tables in the catalog database"
                : "the catalog file " + properties.getDucklake().getCatalogPath();
        return "1) stop yaci-store; 2) delete " + catalog + ", the DuckLake data directory "
                + java.nio.file.Paths.get(properties.getExportPath(), "main") + ", and the rows of analytics_export_state; "
                + "3) start yaci-store; the scheduler re-exports from PostgreSQL.";
    }
}
