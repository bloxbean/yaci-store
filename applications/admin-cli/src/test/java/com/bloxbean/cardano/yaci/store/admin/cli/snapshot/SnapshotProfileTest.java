package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code snapshot} profile group, resolved the way the node and the CLI resolve it: this
 * module's application.yml plus the repository's {@code config/} profile files.
 */
class SnapshotProfileTest {

    private static final Path REPO = Path.of("../..").toAbsolutePath().normalize();

    @Configuration(proxyBeanMethods = false)
    static class Empty {
    }

    @Test
    void activatesLedgerStateAnalyticsAndBlockfrostWithTheSnapshotSettings(@TempDir Path config) throws Exception {
        // The shipped profile files, next to a user's own application.properties that a profile file overrides.
        try (var files = Files.list(REPO.resolve("config"))) {
            for (Path f : files.filter(f -> f.getFileName().toString().startsWith("application-")).toList()) {
                Files.copy(f, config.resolve(f.getFileName()));
            }
        }
        Files.writeString(config.resolve("application.properties"), """
                store.utxo.pruning-enabled=true
                store.transaction.save-full-tx-cbor=false
                """);

        try (ConfigurableApplicationContext ctx = start("--spring.profiles.active=snapshot",
                "--spring.config.additional-location=file:" + config + "/")) {
            Environment env = ctx.getEnvironment();
            assertThat(env.getActiveProfiles())
                    .contains("snapshot", "ledger-state", "analytics", "blockfrost");

            // ledger-state, analytics and blockfrost profile files
            assertThat(env.getProperty("store.adapot.enabled")).isEqualTo("true");
            assertThat(env.getProperty("yaci.store.analytics.enabled")).isEqualTo("true");
            assertThat(env.getProperty("store.extensions.blockfrost.enabled")).isEqualTo("true");
            assertThat(env.getProperty("store.epoch-aggr.enabled")).isEqualTo("true");

            // application-snapshot.properties
            for (String key : new String[]{"store.utxo.pruning-enabled", "store.transaction.pruning-enabled",
                    "store.transaction.cbor-pruning-enabled", "store.blocks.cbor-pruning-enabled",
                    "store.account.pruning-enabled", "store.account.history-cleanup-enabled",
                    "store.adapot.reward-pruning-enabled", "store.adapot.epoch-stake-pruning-enabled"}) {
                assertThat(env.getProperty(key)).as(key).isEqualTo("false");
            }
            assertThat(env.getProperty("store.utxo.save-address")).isEqualTo("true");
            assertThat(env.getProperty("store.transaction.save-full-tx-cbor")).isEqualTo("true");
        }
    }

    @Test
    void theNodeDeclaresTheSameGroupAsTheCli() throws Exception {
        assertThat(group(REPO.resolve("applications/all/src/main/resources/application.yml")))
                .isEqualTo(group(REPO.resolve("applications/admin-cli/src/main/resources/application.yml")))
                .isEqualTo("ledger-state,analytics,blockfrost");
    }

    @Test
    void theDockerProfileFilesMatchTheOnesInConfig() throws Exception {
        for (String file : new String[]{"application-snapshot.properties", "application-blockfrost.properties"}) {
            assertThat(Files.readString(REPO.resolve("docker/config").resolve(file))).as(file)
                    .isEqualTo(Files.readString(REPO.resolve("config").resolve(file)));
        }
    }

    private static ConfigurableApplicationContext start(String... args) {
        SpringApplication app = new SpringApplication(Empty.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        return app.run(args);
    }

    @SuppressWarnings("unchecked")
    private static String group(Path yml) throws Exception {
        try (InputStream in = Files.newInputStream(yml)) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            Map<String, Object> profiles = (Map<String, Object>) spring.get("profiles");
            Map<String, Object> group = (Map<String, Object>) profiles.get("group");
            return (String) group.get("snapshot");
        }
    }
}
