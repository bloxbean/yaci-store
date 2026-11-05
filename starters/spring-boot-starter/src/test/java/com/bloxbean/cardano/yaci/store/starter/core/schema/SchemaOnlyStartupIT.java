package com.bloxbean.cardano.yaci.store.starter.core.schema;

import com.bloxbean.cardano.yaci.store.common.config.SchemaProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "SCHEMA_ONLY_IT_JDBC_URL", matches = ".+")
class SchemaOnlyStartupIT {
    @TempDir Path root;

    private void run(boolean broken) throws Exception {
        String schema = "yaci_schema_only_it_" + UUID.randomUUID().toString().replace("-", "");
        String url = System.getenv("SCHEMA_ONLY_IT_JDBC_URL");
        String user = System.getenv().getOrDefault("SCHEMA_ONLY_IT_USER", "postgres");
        String password = System.getenv().getOrDefault("SCHEMA_ONLY_IT_PASSWORD", "");
        var properties = root.resolve("target.properties");
        // Keep credentials in the temporary config rather than process arguments.
        var config = new java.util.Properties();
        config.setProperty("spring.datasource.url", url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema);
        config.setProperty("spring.datasource.username", user);
        config.setProperty("spring.datasource.password", password);
        config.setProperty("spring.flyway.schemas", schema);
        config.setProperty("spring.flyway.default-schema", schema);
        config.setProperty("store.schema-only", "true");
        config.setProperty("store.sync-auto-start", "true");
        config.setProperty("store.blocks.enabled", "true");
        if (broken) config.setProperty("spring.flyway.locations", "classpath:schema-only-broken");
        try (var out = Files.newOutputStream(properties)) { config.store(out, "isolated schema-only test"); }
        List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-cp", System.getProperty("schemaOnlyTestClasspath"), SchemaOnlyProbeApplication.class.getName(),
                "--spring.config.location=" + properties.toUri()));
        Path log = root.resolve("startup.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try (var conn = DriverManager.getConnection(url, user, password); var st = conn.createStatement()) {
            try {
                assertThat(process.waitFor(45, TimeUnit.SECONDS)).isTrue();
                String output = Files.readString(log);
                assertThat(output).doesNotContain("CONSUMING_APPLICATION_WAS_STARTED", "SCHEMA_ONLY_DID_NOT_EXIT");
                if (broken) {
                    assertThat(process.exitValue()).withFailMessage(output).isNotZero();
                } else {
                    assertThat(process.exitValue()).withFailMessage(output).isZero();
                    assertThat(output).contains("Schema-only initialization complete");
                    assertThat(SchemaProfile.read(conn, schema)).containsEntry("blocks", true)
                            .containsEntry("account", false).containsEntry("adapot", false);
                    try (var rs = st.executeQuery("SELECT count(*) FROM " + schema + ".block")) {
                        assertThat(rs.next()).isTrue(); assertThat(rs.getLong(1)).isZero();
                    }
                }
            } finally {
                process.destroyForcibly();
                st.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
    @Test void createsSchemaAndExitsBeforeAnyApplicationBeans() throws Exception { run(false); }
    @Test void migrationFailureExitsNonzero() throws Exception { run(true); }
}
