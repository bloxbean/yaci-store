package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server settings that decide how fast a bulk load writes WAL.
 *
 * <p>With the default {@code max_wal_size=1GB} the mainnet load checkpointed every few seconds, and
 * after each checkpoint the first change to every page logs a full page image, so the COPY sessions
 * waited on WAL writes. A larger {@code max_wal_size} costs disk for {@code pg_wal} and a longer crash
 * recovery, not memory. None of these settings needs a restart.
 *
 * <p>{@code --tune-postgres} applies them with {@code ALTER SYSTEM} for the import only. The values
 * the server had are recorded in the target schema first, so a failed or resumed import restores
 * the originals, never values an earlier run set.
 */
public final class PostgresTuning {

    public static final String TABLE = "_yaci_snapshot_import_settings";

    /** Setting, recommended value, and the comparable minimum in the setting's own unit. */
    private record Recommendation(String name, String value, long minimum, String why) {}

    private static final List<Recommendation> RECOMMENDED = List.of(
            new Recommendation("max_wal_size", "16GB", 16 * 1024,
                    "avoids a checkpoint every few seconds during the load; costs disk for pg_wal, not memory"),
            new Recommendation("checkpoint_timeout", "15min", 15 * 60,
                    "with a larger max_wal_size the timeout decides how often checkpoints run"),
            new Recommendation("wal_compression", "on", 1,
                    "shrinks the full page images written after each checkpoint"));

    private PostgresTuning() {
    }

    /** One line per setting below its recommendation, for the preflight report. */
    public static List<String> advice(Connection pg) throws SQLException {
        List<String> out = new ArrayList<>();
        for (Recommendation r : below(pg).values()) {
            out.add(String.format("%s is %s; %s or more %s. Pass --tune-postgres to raise it for the import only.",
                    r.name(), current(pg, r.name()), r.value(), r.why()));
        }
        return out;
    }

    /** Null when this session may change the settings, otherwise why it may not. */
    public static String cannotApply(Connection pg) throws SQLException {
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT rolsuper FROM pg_roles WHERE rolname = current_user")) {
            if (rs.next() && rs.getBoolean(1)) {
                return null;
            }
        }
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT has_parameter_privilege('max_wal_size', 'ALTER SYSTEM')"
                        + " AND has_parameter_privilege('checkpoint_timeout', 'ALTER SYSTEM')"
                        + " AND has_parameter_privilege('wal_compression', 'ALTER SYSTEM')")) {
            if (rs.next() && rs.getBoolean(1)) {
                return null;
            }
        } catch (SQLException e) {
            // has_parameter_privilege exists from PostgreSQL 15.
        }
        return "--tune-postgres needs a superuser, or ALTER SYSTEM granted on max_wal_size, checkpoint_timeout"
                + " and wal_compression (PostgreSQL 15+)";
    }

    /**
     * Raises the settings below their recommendation, after recording the server's values once.
     *
     * @return what was changed, for the import log
     */
    static List<String> apply(Connection pg, String schema) throws SQLException {
        Map<String, Recommendation> below = below(pg);
        if (!exists(pg, schema)) {
            try (Statement st = pg.createStatement()) {
                st.execute("CREATE TABLE " + table(schema) + " (name text PRIMARY KEY, auto_conf_value text)");
            }
            try (PreparedStatement ps = pg.prepareStatement("INSERT INTO " + table(schema) + " VALUES (?, ?)")) {
                for (Recommendation r : RECOMMENDED) {
                    ps.setString(1, r.name());
                    ps.setString(2, autoConfValue(pg, r.name()));
                    ps.executeUpdate();
                }
            }
        }
        List<String> changed = new ArrayList<>();
        try (Statement st = pg.createStatement()) {
            for (Recommendation r : below.values()) {
                changed.add(r.name() + " " + current(pg, r.name()) + " -> " + r.value());
                st.execute("ALTER SYSTEM SET " + r.name() + " = '" + r.value() + "'");
            }
            if (!changed.isEmpty()) {
                st.execute("SELECT pg_reload_conf()");
            }
        }
        return changed;
    }

    /** Puts back what {@link #apply} recorded and forgets it. Does nothing when nothing was applied. */
    static void restore(Connection pg, String schema) throws SQLException {
        if (!exists(pg, schema)) {
            return;
        }
        Map<String, String> original = new LinkedHashMap<>();
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT name, auto_conf_value FROM " + table(schema) + " ORDER BY name")) {
            while (rs.next()) {
                original.put(rs.getString(1), rs.getString(2));
            }
        }
        try (Statement st = pg.createStatement()) {
            for (Map.Entry<String, String> e : original.entrySet()) {
                Identifiers.requireSqlIdentifier(e.getKey(), "setting name");
                st.execute(e.getValue() == null ? "ALTER SYSTEM RESET " + e.getKey()
                        : "ALTER SYSTEM SET " + e.getKey() + " = '" + e.getValue().replace("'", "''") + "'");
            }
            st.execute("SELECT pg_reload_conf()");
            st.execute("DROP TABLE " + table(schema));
        }
    }

    /** True while settings applied by an import have not been restored. */
    public static boolean pending(Connection pg, String schema) throws SQLException {
        return exists(pg, schema);
    }

    private static Map<String, Recommendation> below(Connection pg) throws SQLException {
        Map<String, Recommendation> out = new LinkedHashMap<>();
        for (Recommendation r : RECOMMENDED) {
            try (PreparedStatement ps = pg.prepareStatement("SELECT setting FROM pg_settings WHERE name = ?")) {
                ps.setString(1, r.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        continue;
                    }
                    String setting = rs.getString(1);
                    long value = "wal_compression".equals(r.name()) ? ("off".equals(setting) ? 0 : 1)
                            : Long.parseLong(setting);
                    if (value < r.minimum()) {
                        out.put(r.name(), r);
                    }
                }
            }
        }
        return out;
    }

    private static String current(Connection pg, String name) throws SQLException {
        try (PreparedStatement ps = pg.prepareStatement("SELECT current_setting(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "unset";
            }
        }
    }

    /** The value postgresql.auto.conf holds for the setting, or null when it holds none. */
    private static String autoConfValue(Connection pg, String name) throws SQLException {
        try (PreparedStatement ps = pg.prepareStatement("SELECT setting, unit FROM pg_settings"
                + " WHERE name = ? AND sourcefile LIKE '%postgresql.auto.conf'")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) + (rs.getString(2) == null ? "" : rs.getString(2)) : null;
            }
        }
    }

    private static boolean exists(Connection pg, String schema) throws SQLException {
        try (PreparedStatement ps = pg.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            ps.setString(1, table(schema));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private static String table(String schema) {
        return Identifiers.quote(schema) + "." + Identifiers.quote(TABLE);
    }
}
