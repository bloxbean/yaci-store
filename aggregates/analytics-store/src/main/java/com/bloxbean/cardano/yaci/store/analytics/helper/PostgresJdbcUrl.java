package com.bloxbean.cardano.yaci.store.analytics.helper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A parsed PgJDBC URL ({@code jdbc:postgresql:...}), following the rules of PgJDBC's own parser,
 * and its translation to libpq connection parameters for DuckDB's {@code postgres_scanner} and
 * PostgreSQL-backed DuckLake catalogs.
 *
 * <p>Supported forms: {@code jdbc:postgresql:db}, {@code jdbc:postgresql:/},
 * {@code jdbc:postgresql://host[:port]/[db]}, IPv6 hosts in brackets ({@code [::1]:5432}) and
 * multiple hosts ({@code h1:5432,h2:5433}). Parameter values are URL-decoded.</p>
 *
 * @param hosts      host names (never empty; {@code localhost} by default)
 * @param ports      ports, one per host ({@code 5432} by default)
 * @param database   database name, empty for the server default
 * @param parameters URL parameters, decoded, in URL order (names as written)
 */
public record PostgresJdbcUrl(List<String> hosts, List<String> ports, String database,
                              Map<String, String> parameters) {

    private static final String PREFIX = "jdbc:postgresql:";
    private static final String DEFAULT_PORT = "5432";

    public PostgresJdbcUrl {
        hosts = List.copyOf(hosts);
        ports = List.copyOf(ports);
        parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }

    /**
     * Parse a PgJDBC URL.
     *
     * @throws IllegalArgumentException if the URL is not a {@code jdbc:postgresql:} URL or is malformed
     */
    public static PostgresJdbcUrl parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("no JDBC URL configured");
        }
        String trimmed = url.trim();
        if (!trimmed.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            throw new IllegalArgumentException("not a PostgreSQL JDBC URL (expected jdbc:postgresql:...)");
        }
        String rest = trimmed.substring(PREFIX.length());
        String server = rest;
        String query = "";
        int q = rest.indexOf('?');
        if (q >= 0) {
            server = rest.substring(0, q);
            query = rest.substring(q + 1);
        }

        List<String> hosts = new ArrayList<>();
        List<String> ports = new ArrayList<>();
        String database;
        if (server.startsWith("//")) {
            server = server.substring(2);
            int slash = server.indexOf('/');
            String addresses = slash >= 0 ? server.substring(0, slash) : server;
            database = slash >= 0 ? decode(server.substring(slash + 1)) : "";
            for (String address : addresses.split(",", -1)) {
                parseAddress(address.trim(), hosts, ports);
            }
        } else {
            database = decode(server.startsWith("/") ? server.substring(1) : server);
            hosts.add("localhost");
            ports.add(DEFAULT_PORT);
        }

        Map<String, String> parameters = new LinkedHashMap<>();
        if (!query.isEmpty()) {
            for (String pair : query.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String key = decode(eq >= 0 ? pair.substring(0, eq) : pair);
                String value = eq >= 0 ? decode(pair.substring(eq + 1)) : "";
                parameters.put(key, value);
            }
        }
        return new PostgresJdbcUrl(hosts, ports, database, parameters);
    }

    private static void parseAddress(String address, List<String> hosts, List<String> ports) {
        String host;
        String port = DEFAULT_PORT;
        if (address.startsWith("[")) {
            int close = address.indexOf(']');
            if (close < 0) {
                throw new IllegalArgumentException("unterminated IPv6 address '" + address + "'");
            }
            host = address.substring(1, close);
            String after = address.substring(close + 1);
            if (after.startsWith(":") && after.length() > 1) {
                port = after.substring(1);
            } else if (!after.isEmpty()) {
                throw new IllegalArgumentException("invalid host address '" + address + "'");
            }
        } else {
            int colon = address.lastIndexOf(':');
            if (colon >= 0) {
                host = address.substring(0, colon);
                if (colon + 1 < address.length()) {
                    port = address.substring(colon + 1);
                }
            } else {
                host = address;
            }
        }
        if (!port.chars().allMatch(Character::isDigit) || port.isEmpty()) {
            throw new IllegalArgumentException("invalid port '" + port + "' in host address '" + address + "'");
        }
        hosts.add(host.isEmpty() ? "localhost" : host);
        ports.add(port);
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value; // PgJDBC also keeps undecodable values as they are
        }
    }

    /** Case-insensitive parameter lookup ({@code currentSchema}, {@code sslmode}, ...). */
    public String parameter(String name) {
        for (Map.Entry<String, String> e : parameters.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    /** libpq {@code host} value (comma separated for several hosts). */
    public String libpqHost() {
        return String.join(",", hosts);
    }

    /** libpq {@code port} value (comma separated for several hosts). */
    public String libpqPort() {
        return String.join(",", ports);
    }

    /**
     * Connection parameters that libpq understands, translated from their PgJDBC names, in a
     * stable order. Credentials, the database, hosts, {@code currentSchema} and {@code options}
     * are not included (the caller handles them).
     *
     * <ul>
     *   <li>{@code sslmode}, {@code sslrootcert}, {@code sslcert}, {@code sslkey},
     *       {@code sslpassword}: same names; {@code ssl=true} without {@code sslmode} means
     *       {@code verify-full}, as in PgJDBC.</li>
     *   <li>{@code targetServerType} &rarr; {@code target_session_attrs}
     *       ({@code primary}/{@code master} &rarr; {@code primary}, {@code secondary}/{@code slave}
     *       &rarr; {@code standby}, {@code preferSecondary} &rarr; {@code prefer-standby},
     *       {@code any} &rarr; {@code any}).</li>
     *   <li>{@code connectTimeout} &rarr; {@code connect_timeout},
     *       {@code ApplicationName} &rarr; {@code application_name},
     *       {@code gssEncMode} &rarr; {@code gssencmode},
     *       {@code channelBinding} &rarr; {@code channel_binding}.</li>
     * </ul>
     *
     * @param ignored receives the names of URL parameters that have no libpq equivalent
     */
    public Map<String, String> libpqParameters(List<String> ignored) {
        Map<String, String> out = new LinkedHashMap<>();
        String sslmode = parameter("sslmode");
        String ssl = parameter("ssl");
        if (sslmode != null && !sslmode.isBlank()) {
            out.put("sslmode", sslmode);
        } else if (ssl != null && (ssl.isEmpty() || "true".equalsIgnoreCase(ssl))) {
            out.put("sslmode", "verify-full");
        }
        for (String name : List.of("sslrootcert", "sslcert", "sslkey", "sslpassword")) {
            String value = parameter(name);
            if (value != null && !value.isEmpty()) {
                out.put(name, value);
            }
        }
        String target = parameter("targetServerType");
        if (target != null && !target.isBlank()) {
            String mapped = switch (target.trim().toLowerCase(Locale.ROOT)) {
                case "any" -> "any";
                case "primary", "master" -> "primary";
                case "secondary", "slave" -> "standby";
                case "prefersecondary", "preferslave" -> "prefer-standby";
                default -> null;
            };
            if (mapped != null) {
                out.put("target_session_attrs", mapped);
            } else if (ignored != null) {
                ignored.add("targetServerType=" + target);
            }
        }
        putIfPresent(out, "connect_timeout", parameter("connectTimeout"));
        putIfPresent(out, "application_name", parameter("ApplicationName"));
        putIfPresent(out, "gssencmode", parameter("gssEncMode"));
        putIfPresent(out, "channel_binding", parameter("channelBinding"));

        if (ignored != null) {
            for (String name : parameters.keySet()) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("ssl") && !out.containsKey(lower) && !"ssl".equals(lower)
                        && !"sslmode".equals(lower)) {
                    ignored.add(name); // sslfactory, sslhostnameverifier, ...: Java only
                }
            }
        }
        return out;
    }

    private static void putIfPresent(Map<String, String> out, String key, String value) {
        if (value != null && !value.isBlank()) {
            out.put(key, value.trim());
        }
    }
}
