package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class DuckPgSessionTest {
    private ImportOptions options(String url, String password) {
        return new ImportOptions(Path.of("manifest"), Path.of("."), Path.of("."), url,
                "user name", password, "public", "preprod", 1, 1, 10, 1, "512MB", 0,
                true, false, List.of(), false, false);
    }

    @Test
    void quotesCredentialsAndPreservesIpv6AndTlsSettings() {
        String connection = DuckPgSession.pgConnectionString(options(
                "jdbc:postgresql://[::1]:5433/my%20db?currentSchema=restore&sslmode=verify-full&sslrootcert=%2Ftmp%2Froot.crt",
                "has space'and\\slash"));
        assertThat(connection).contains("host='::1'", "port='5433'", "dbname='my db'",
                "user='user name'", "password='has space\\'and\\\\slash'",
                "sslmode='verify-full'", "sslrootcert='/tmp/root.crt'");
        assertThat(connection).doesNotContain("currentSchema");
    }

    @Test
    void usesJdbcCredentialPrecedenceAndSslSemantics() {
        assertThat(DuckPgSession.pgConnectionString(options(
                "jdbc:postgresql://localhost/db?user=urluser&password=urlpass&ssl=true", "default")))
                .contains("user='urluser'", "password='urlpass'", "sslmode='verify-full'");
    }

    @Test
    void redactsQuotedAndSqlEscapedPasswords() {
        String password = "has space'and\\slash";
        String connection = DuckPgSession.pgConnectionString(options("jdbc:postgresql://localhost/db", password));
        assertThat(DuckPgSession.redact(connection, password)).doesNotContain("space", "slash");
        assertThat(DuckPgSession.redact(connection.replace("'", "''"), password))
                .doesNotContain("space", "slash");
        assertThat(DuckPgSession.redact("password='url password' host='localhost'", null))
                .doesNotContain("url password").contains("host='localhost'");
    }

    @Test
    void rejectsUnsupportedConnectionSettingsBeforeImport() {
        assertThatThrownBy(() -> DuckPgSession.pgConnectionString(options(
                "jdbc:postgresql://localhost/db?sslfactory=custom.Factory", "secret")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sslfactory")
                .hasMessageNotContaining("secret");
        assertThatThrownBy(() -> DuckPgSession.pgConnectionString(options(
                "jdbc:postgresql://host1,host2/db", "secret")))
                .hasMessageContaining("single PostgreSQL host");
    }
}
