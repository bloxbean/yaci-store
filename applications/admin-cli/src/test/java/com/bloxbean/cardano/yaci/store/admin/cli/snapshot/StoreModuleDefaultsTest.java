package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.common.config.StoreModuleConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class StoreModuleDefaultsTest {
    @Test
    void defaultScopeAndOverridesMatchStoreFlags() {
        var env = new MockEnvironment();
        var defaults = StoreModuleConfig.configured(env);
        assertThat(defaults).containsEntry("blocks", true).containsEntry("utxo", true)
                .containsEntry("account", false).containsEntry("adapot", false)
                .containsEntry("epoch-aggr", false).containsEntry("governance-aggr", false);
        env.setProperty("store.account.enabled", "true");
        env.setProperty("store.utxo.enabled", "false");
        env.setProperty("store.assets.ext.enabled", "true");
        assertThat(StoreModuleConfig.configured(env)).containsEntry("account", true)
                .containsEntry("utxo", false).containsEntry("assets-ext", true);
    }
}
