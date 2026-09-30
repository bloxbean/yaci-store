package com.bloxbean.cardano.yaci.store.snapshot.ducklake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

class DuckLakeCatalogTest {
    @TempDir Path root;

    @Test
    void refusesLiveWriterInsteadOfReadingAnOutdatedFileCopy() throws Exception {
        Path source = root.resolve("CatalogWriter.java");
        Files.writeString(source, """
                import java.sql.*;
                class CatalogWriter {
                    public static void main(String[] args) throws Exception {
                        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + args[0])) {
                            var st = conn.createStatement();
                            st.execute("CREATE TABLE marker(i INTEGER)");
                            st.execute("INSERT INTO marker VALUES (1)");
                            st.execute("CHECKPOINT");
                            st.execute("INSERT INTO marker VALUES (2)");
                            System.out.println("ready");
                            System.out.flush();
                            System.in.read();
                        }
                    }
                }
                """);
        String driver = Path.of(org.duckdb.DuckDBDriver.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toString();
        Process writer = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "--class-path", driver, source.toString(), root.resolve("ducklake.catalog.db").toString())
                .redirectErrorStream(true).start();
        try {
            var output = new BufferedReader(new InputStreamReader(writer.getInputStream()));
            assertThat(output.readLine()).isEqualTo("ready");
            assertThatThrownBy(() -> DuckLakeCatalog.open(root, root.resolve("work")))
                    .hasMessageContaining("Stop analytics exports");
            writer.getOutputStream().close();
            assertThat(writer.waitFor(10, TimeUnit.SECONDS)).isTrue();
            assertThat(writer.exitValue()).isZero();
            try (DuckLakeCatalog catalog = DuckLakeCatalog.open(root, root.resolve("work"));
                 var rs = catalog.connection().createStatement().executeQuery("SELECT count(*) FROM snapcat.marker")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        } finally {
            writer.destroyForcibly();
        }
    }
}
