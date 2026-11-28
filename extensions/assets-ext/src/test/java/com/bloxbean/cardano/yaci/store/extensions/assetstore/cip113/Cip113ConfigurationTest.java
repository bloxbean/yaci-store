package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cip113Configuration")
class Cip113ConfigurationTest {

    private static final long MAINNET = 764824073L;
    private static final long PREPROD = 1L;
    private static final long PREVIEW = 2L;
    private static final long DEVKIT = 42L;

    private static Cip113Configuration resolve(long protocolMagic, boolean enabled, List<String> policyIds) {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(protocolMagic);
        AssetsExtStoreProperties assetsProps = new AssetsExtStoreProperties();
        assetsProps.getCip113().setEnabled(enabled);
        assetsProps.getCip113().setRegistryNftPolicyIds(policyIds);
        Cip113Configuration configuration = new Cip113Configuration(storeProperties, assetsProps);
        configuration.resolve();
        return configuration;
    }

    @Test
    @DisplayName("is on by default")
    void enabledByDefault() {
        assertThat(new AssetsExtStoreProperties().getCip113().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("bundles the registry of each public network")
    void bundledPerNetwork() {
        assertThat(resolve(MAINNET, true, List.of()).getRegistryNftPolicyIds())
                .containsExactly("484e733d122af44e6101988bcc47ed261a7af5c43c797e89d60e3075");
        assertThat(resolve(PREPROD, true, List.of()).getRegistryNftPolicyIds())
                .containsExactly("3083d387537f9318b6ff5aeab7de5f5629d26495319b8fafd409222a");
        assertThat(resolve(PREVIEW, true, List.of()).getRegistryNftPolicyIds())
                .containsExactly("e5b339ef5b16d6c460759aca1d60e5e045a6f01da4a11b4ee0fec09a");
    }

    @Test
    @DisplayName("has no registry on another network, so every token is native")
    void unknownNetwork() {
        Cip113Configuration configuration = resolve(DEVKIT, true, List.of());

        assertThat(configuration.isEnabled()).isFalse();
        assertThat(configuration.isRegistryPolicyId("484e733d122af44e6101988bcc47ed261a7af5c43c797e89d60e3075")).isFalse();
    }

    @Test
    @DisplayName("the configured policy IDs replace the bundled ones, trimmed and in lower case")
    void override() {
        Cip113Configuration configuration = resolve(MAINNET, true, List.of(" AABB" + "cc".repeat(26) + " ", ""));

        assertThat(configuration.getRegistryNftPolicyIds()).containsExactly("aabb" + "cc".repeat(26));
        assertThat(configuration.isRegistryPolicyId("AABB" + "CC".repeat(26))).isTrue();
        assertThat(configuration.isRegistryPolicyId("484e733d122af44e6101988bcc47ed261a7af5c43c797e89d60e3075")).isFalse();
        assertThat(resolve(DEVKIT, true, List.of("aa".repeat(28))).isEnabled()).isTrue();
    }

    @Test
    @DisplayName("switched off, it follows no registry, not even a configured one")
    void disabled() {
        Cip113Configuration configuration = resolve(MAINNET, false, List.of("aa".repeat(28)));

        assertThat(configuration.isEnabled()).isFalse();
        assertThat(configuration.getRegistryNftPolicyIds()).isEmpty();
    }
}
