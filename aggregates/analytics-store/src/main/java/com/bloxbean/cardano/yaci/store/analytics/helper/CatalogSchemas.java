package com.bloxbean.cardano.yaci.store.analytics.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Chooses the PostgreSQL schema that holds the DuckLake catalog tables ({@code ducklake_*}) and
 * guards against two yaci-store instances sharing one catalog.
 *
 * <p>Before {@code yaci.store.analytics.ducklake.catalog-schema} existed, the catalog was always
 * created in {@code public}. Two instances indexing into different schemas of one database
 * therefore shared one catalog and overwrote each other's partitions, and a user without
 * {@code CREATE} on {@code public} could not start. The rules:</p>
 * <ol>
 *   <li><b>{@code catalog-schema} set:</b> that schema is used. If it has no catalog yet while
 *       {@code public} has one that belongs to this instance, startup is refused: switching would
 *       silently start an empty catalog while the export state says every partition is done.</li>
 *   <li><b>{@code catalog-schema} not set:</b> the schema of the catalog connection
 *       ({@code currentSchema} of {@code catalog-url}, else of {@code spring.datasource.url}) if it
 *       already has a catalog; otherwise an existing catalog in {@code public} (installations
 *       created before this setting, kept as they are); otherwise the connection's schema, where a
 *       new catalog is created.</li>
 *   <li><b>Ownership.</b> The catalog records which source ({@code <database>/<schema>} of
 *       {@code spring.datasource.url}) it belongs to, in its {@code ducklake_metadata} table
 *       under {@value #OWNER_KEY}. A catalog that already records a different source is refused.
 *       A catalog without the record (older installations) is claimed by the first instance
 *       that writes to it.</li>
 * </ol>
 */
public final class CatalogSchemas {

    /** {@code ducklake_metadata} key recording the source a catalog belongs to. */
    public static final String OWNER_KEY = "yaci_store_source";

    public static final String PUBLIC = "public";

    private CatalogSchemas() {
    }

    /** What the catalog database looks like (looked up through the {@code catalog_temp} attach). */
    public interface CatalogLookup {
        /** True if {@code schema.ducklake_metadata} exists. */
        boolean hasCatalog(String schema) throws java.sql.SQLException;

        /** A global {@code ducklake_metadata} value of the catalog in {@code schema}, or null. */
        String metadata(String schema, String key) throws java.sql.SQLException;
    }

    /**
     * @param schema       the schema to use for {@code METADATA_SCHEMA}
     * @param exists       whether that schema already holds a catalog
     * @param claimOwner   true when the catalog is new or does not record its owner yet; this
     *                     instance records it after the first read-write attach
     * @param legacyPublic true when an unset {@code catalog-schema} fell back to an existing
     *                     catalog in {@code public}
     */
    public record Resolution(String schema, boolean exists, boolean claimOwner, boolean legacyPublic) {
    }

    /**
     * Decide the catalog schema.
     *
     * @param configured    {@code catalog-schema}, or null/blank when not set
     * @param connectionSchema the schema of the catalog connection ({@code currentSchema})
     * @param owner         this instance's source identity ({@link #ownerIdentity})
     * @param exportPath    this instance's export path (DuckLake {@code DATA_PATH})
     * @throws IllegalStateException with an actionable message when the catalog must not be used
     */
    public static Resolution resolve(String configured, String connectionSchema, String owner, String exportPath,
                                     CatalogLookup lookup) throws java.sql.SQLException {
        String fallback = connectionSchema == null || connectionSchema.isBlank() ? PUBLIC : connectionSchema;
        String schema;
        boolean legacyPublic = false;
        if (configured != null && !configured.isBlank()) {
            schema = configured.trim();
            if (!lookup.hasCatalog(schema) && !PUBLIC.equals(schema) && lookup.hasCatalog(PUBLIC)
                    && belongsTo(lookup, PUBLIC, owner, exportPath)) {
                throw new IllegalStateException("yaci.store.analytics.ducklake.catalog-schema=" + schema
                        + " has no DuckLake catalog yet, but this instance's existing catalog is in schema 'public' ("
                        + describe(lookup, PUBLIC) + "). Starting with an empty catalog would skip every partition "
                        + "already marked as exported. Keep using it with catalog-schema=public (or leave the "
                        + "property unset), or move it first: ALTER TABLE public.ducklake_<table> SET SCHEMA "
                        + schema + " for every ducklake_* table.");
            }
        } else if (lookup.hasCatalog(fallback)) {
            schema = fallback;
        } else if (!PUBLIC.equals(fallback) && lookup.hasCatalog(PUBLIC)) {
            schema = PUBLIC;
            legacyPublic = true;
        } else {
            schema = fallback;
        }

        boolean exists = lookup.hasCatalog(schema);
        boolean claim = !exists; // a new catalog records its owner right after it is created
        if (exists) {
            String recorded = lookup.metadata(schema, OWNER_KEY);
            if (recorded == null || recorded.isBlank()) {
                if (legacyPublic && !sameDirectory(lookup.metadata(schema, "data_path"), exportPath)) {
                    throw new IllegalStateException("yaci.store.analytics.ducklake.catalog-schema is not set and "
                            + "schema '" + fallback + "' has no DuckLake catalog. The existing catalog in schema "
                            + "'public' (" + describe(lookup, PUBLIC) + ") was written for a different export path "
                            + "than this instance's (" + exportPath + "), so it probably belongs to another "
                            + "instance. Set yaci.store.analytics.ducklake.catalog-schema to a schema of this "
                            + "instance's own (for example " + fallback + ") to create a new catalog there, or "
                            + "set it to public if that catalog is this instance's.");
                }
                claim = true;
            } else if (!recorded.equals(owner)) {
                throw new IllegalStateException("The DuckLake catalog in schema '" + schema + "' belongs to another "
                        + "yaci-store instance (source " + recorded + ", " + describe(lookup, schema)
                        + "); this instance indexes " + owner + ". Two instances must not share one catalog: they "
                        + "overwrite each other's partitions. Set yaci.store.analytics.ducklake.catalog-schema to a "
                        + "schema of this instance's own (for example its source schema). If the catalog does "
                        + "belong to this instance (database or schema renamed), update the record: UPDATE \""
                        + schema + "\".ducklake_metadata SET value = '" + owner + "' WHERE key = '" + OWNER_KEY + "'.");
            }
        }
        return new Resolution(schema, exists, claim, legacyPublic);
    }

    /** {@code <database>/<schema>} of the source datasource. */
    public static String ownerIdentity(String database, String schema) {
        return Objects.toString(database, "") + "/" + Objects.toString(schema, PUBLIC);
    }

    /**
     * Whether the catalog in {@code schema} is this instance's: it records {@code owner}, or it records no
     * owner (older installations) and was written for {@code exportPath}.
     */
    public static boolean belongsTo(CatalogLookup lookup, String schema, String owner, String exportPath)
            throws java.sql.SQLException {
        String recorded = lookup.metadata(schema, OWNER_KEY);
        if (recorded != null && !recorded.isBlank()) {
            return recorded.equals(owner);
        }
        return sameDirectory(lookup.metadata(schema, "data_path"), exportPath);
    }

    private static String describe(CatalogLookup lookup, String schema) throws java.sql.SQLException {
        return "DATA_PATH " + lookup.metadata(schema, "data_path");
    }

    /**
     * True if both paths name the same directory: equal after making them absolute and
     * normalizing, or equal real paths (symbolic links, {@code ./x} vs {@code /app/x}).
     * Relative paths are resolved against the working directory, as DuckLake does.
     */
    public static boolean sameDirectory(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        Path pa = normalize(a);
        Path pb = normalize(b);
        if (pa.equals(pb)) {
            return true;
        }
        try {
            if (Files.exists(pa) && Files.exists(pb)) {
                return Files.isSameFile(pa, pb);
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private static Path normalize(String path) {
        String p = path.trim();
        while (p.length() > 1 && (p.endsWith("/") || p.endsWith("\\"))) {
            p = p.substring(0, p.length() - 1);
        }
        return Paths.get(p).toAbsolutePath().normalize();
    }
}
