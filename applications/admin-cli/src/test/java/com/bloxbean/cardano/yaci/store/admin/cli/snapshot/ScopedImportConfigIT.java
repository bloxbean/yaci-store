package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.common.config.SchemaProfile;
import com.bloxbean.cardano.yaci.store.common.config.StoreModuleConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.env.MockEnvironment;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class ScopedImportConfigIT {
    @Test
    void cliUsesConsumerProfileAndRejectsConflictingFlags() throws Exception {
        String schema = "yaci_scope_config_it_" + UUID.randomUUID().toString().replace("-", "");
        String url = System.getenv("SNAPSHOT_IT_JDBC_URL");
        String user = System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres");
        String password = System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", "");
        var env = new MockEnvironment()
                .withProperty("spring.datasource.url", url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema)
                .withProperty("spring.datasource.username", user).withProperty("spring.datasource.password", password);
        var support = new SnapshotCliSupport(env);
        try (var conn = DriverManager.getConnection(url, user, password); var st = conn.createStatement()) {
            try {
                st.execute("CREATE SCHEMA " + schema);
                assertThatThrownBy(() -> support.importRegistry(null, false)).hasMessageContaining("store.schema-only=true");
                Map<String, Boolean> profile = new java.util.TreeMap<>();
                StoreModuleConfig.MODULES.keySet().forEach(id -> profile.put(id, false));
                profile.put("core", true); profile.put("blocks", true);
                SchemaProfile.write(conn, schema, profile);
                var selected = support.importRegistry(null, false);
                assertThat(selected.isSelected("block")).isTrue();
                assertThat(selected.isSelected("adapot")).isFalse();
                assertThat(selected.isSelected("address-utxo")).isFalse();
                env.setProperty("store.account.enabled", "true");
                assertThatThrownBy(() -> support.importRegistry(null, false)).hasMessageContaining("differs from the target");
            } finally {
                st.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
