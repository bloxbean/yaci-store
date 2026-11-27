package com.bloxbean.cardano.yaci.store.analytics.helper;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresJdbcUrlTest {

    @Test
    void parsesHostPortDatabaseAndParameters() {
        PostgresJdbcUrl url = PostgresJdbcUrl.parse(
                "jdbc:postgresql://db.example:5433/yaci_store?currentSchema=mainnet&sslrootcert=/a/root.crt");
        assertEquals(List.of("db.example"), url.hosts());
        assertEquals(List.of("5433"), url.ports());
        assertEquals("yaci_store", url.database());
        assertEquals("mainnet", url.parameter("currentSchema"));
        assertEquals("/a/root.crt", url.parameter("sslrootcert"));
    }

    @Test
    void slashInAParameterValueDoesNotChangeTheDatabase() {
        // The old parser took the text after the last '/' as the database: "root.crt"
        PostgresJdbcUrl url = PostgresJdbcUrl.parse(
                "jdbc:postgresql://localhost/yaci?sslrootcert=/etc/ssl/root.crt&currentSchema=preprod");
        assertEquals("yaci", url.database());
        assertEquals("5432", url.libpqPort());
    }

    @Test
    void ipv6AndMultipleHosts() {
        PostgresJdbcUrl v6 = PostgresJdbcUrl.parse("jdbc:postgresql://[::1]:5433/yaci");
        assertEquals("::1", v6.libpqHost());
        assertEquals("5433", v6.libpqPort());

        PostgresJdbcUrl v6NoPort = PostgresJdbcUrl.parse("jdbc:postgresql://[2001:db8::7]/yaci");
        assertEquals("2001:db8::7", v6NoPort.libpqHost());
        assertEquals("5432", v6NoPort.libpqPort());

        PostgresJdbcUrl multi = PostgresJdbcUrl.parse("jdbc:postgresql://pg1:5432,pg2:5433,pg3/yaci?targetServerType=primary");
        assertEquals("pg1,pg2,pg3", multi.libpqHost());
        assertEquals("5432,5433,5432", multi.libpqPort());
        assertEquals("primary", multi.libpqParameters(null).get("target_session_attrs"));
    }

    @Test
    void shortFormsWithoutHost() {
        PostgresJdbcUrl db = PostgresJdbcUrl.parse("jdbc:postgresql:yaci");
        assertEquals("yaci", db.database());
        assertEquals("localhost", db.libpqHost());
        assertEquals("5432", db.libpqPort());

        PostgresJdbcUrl root = PostgresJdbcUrl.parse("jdbc:postgresql:/");
        assertEquals("", root.database());

        PostgresJdbcUrl noDb = PostgresJdbcUrl.parse("jdbc:postgresql://host/");
        assertEquals("", noDb.database());
        assertEquals("host", noDb.libpqHost());
    }

    @Test
    void decodesValues() {
        PostgresJdbcUrl url = PostgresJdbcUrl.parse(
                "jdbc:postgresql://h/my%20db?password=p%26w%3Dx&ApplicationName=yaci%20store");
        assertEquals("my db", url.database());
        assertEquals("p&w=x", url.parameter("password"));
        assertEquals("yaci store", url.libpqParameters(null).get("application_name"));
    }

    @Test
    void rejectsNonPostgresUrls() {
        assertThrows(IllegalArgumentException.class, () -> PostgresJdbcUrl.parse("jdbc:h2:mem:test;MODE=PostgreSQL"));
        assertThrows(IllegalArgumentException.class, () -> PostgresJdbcUrl.parse(null));
        assertThrows(IllegalArgumentException.class, () -> PostgresJdbcUrl.parse("jdbc:postgresql://h:port/db"));
        assertThrows(IllegalArgumentException.class, () -> PostgresJdbcUrl.parse("jdbc:postgresql://[::1/db"));
    }

    @Test
    void translatesSslAndConnectionParametersToLibpq() {
        List<String> ignored = new ArrayList<>();
        Map<String, String> libpq = PostgresJdbcUrl.parse("jdbc:postgresql://h/db?sslmode=verify-ca"
                        + "&sslrootcert=/r.crt&sslcert=/c.crt&sslkey=/k.key&sslpassword=kp&sslfactory=x.Y"
                        + "&connectTimeout=7&gssEncMode=disable&channelBinding=require&targetServerType=preferSecondary")
                .libpqParameters(ignored);
        assertEquals("verify-ca", libpq.get("sslmode"));
        assertEquals("/r.crt", libpq.get("sslrootcert"));
        assertEquals("/c.crt", libpq.get("sslcert"));
        assertEquals("/k.key", libpq.get("sslkey"));
        assertEquals("kp", libpq.get("sslpassword"));
        assertEquals("7", libpq.get("connect_timeout"));
        assertEquals("disable", libpq.get("gssencmode"));
        assertEquals("require", libpq.get("channel_binding"));
        assertEquals("prefer-standby", libpq.get("target_session_attrs"));
        assertEquals(List.of("sslfactory"), ignored);
    }

    @Test
    void sslTrueWithAJavaSslFactoryMeansRequire() {
        // A managed database with a self-signed CA: PgJDBC trusts any certificate through the factory.
        // libpq cannot run it, and verify-full would break a connection that works today.
        List<String> ignored = new java.util.ArrayList<>();
        assertEquals("require", PostgresJdbcUrl.parse("jdbc:postgresql://db:25060/yaci?ssl=true"
                        + "&sslfactory=org.postgresql.ssl.NonValidatingFactory&currentSchema=mainnet")
                .libpqParameters(ignored).get("sslmode"));
        assertEquals(List.of("sslfactory"), ignored);
        assertEquals("require", PostgresJdbcUrl.parse("jdbc:postgresql://h/db?ssl=true&sslhostnameverifier=x.Y")
                .libpqParameters(null).get("sslmode"));
        // An explicit sslmode still wins
        assertEquals("verify-full", PostgresJdbcUrl.parse("jdbc:postgresql://h/db?ssl=true&sslmode=verify-full"
                        + "&sslfactory=org.postgresql.ssl.NonValidatingFactory")
                .libpqParameters(null).get("sslmode"));
    }

    @Test
    void sslTrueMeansVerifyFullAsInPgJdbc() {
        assertEquals("verify-full", PostgresJdbcUrl.parse("jdbc:postgresql://h/db?ssl=true")
                .libpqParameters(null).get("sslmode"));
        assertEquals("verify-full", PostgresJdbcUrl.parse("jdbc:postgresql://h/db?ssl")
                .libpqParameters(null).get("sslmode"));
        assertEquals("require", PostgresJdbcUrl.parse("jdbc:postgresql://h/db?ssl=true&sslmode=require")
                .libpqParameters(null).get("sslmode"));
        assertNull(PostgresJdbcUrl.parse("jdbc:postgresql://h/db?ssl=false").libpqParameters(null).get("sslmode"));
        assertTrue(PostgresJdbcUrl.parse("jdbc:postgresql://h/db").libpqParameters(null).isEmpty());
    }
}
