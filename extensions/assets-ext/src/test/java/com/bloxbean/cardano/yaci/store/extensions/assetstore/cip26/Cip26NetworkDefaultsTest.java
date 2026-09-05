package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cip26NetworkDefaults")
class Cip26NetworkDefaultsTest {

    private static final long MAINNET_MAGIC = 764824073L;
    private static final long PREPROD_MAGIC = 1L;
    private static final long PREVIEW_MAGIC = 2L;
    private static final long SANCHONET_MAGIC = 4L;
    private static final long DEVKIT_MAGIC = 42L;

    private static Cip26NetworkDefaults resolve(long protocolMagic, AssetsExtStoreProperties.Cip26 cip26) {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(protocolMagic);
        AssetsExtStoreProperties assetsProps = new AssetsExtStoreProperties();
        if (cip26 != null) {
            assetsProps.setCip26(cip26);
        }
        Cip26NetworkDefaults defaults = new Cip26NetworkDefaults(storeProperties, assetsProps);
        defaults.resolve();
        return defaults;
    }

    @Nested
    @DisplayName("default resolution per network (no user override)")
    class DefaultsPerNetwork {

        @Test
        void mainnetUsesCardanoFoundation() {
            Cip26NetworkDefaults defaults = resolve(MAINNET_MAGIC, null);

            assertThat(defaults.isRegistryAvailable()).isTrue();
            assertThat(defaults.getOrganization()).isEqualTo("cardano-foundation");
            assertThat(defaults.getProjectName()).isEqualTo("cardano-token-registry");
            assertThat(defaults.getMappingsFolder()).isEqualTo("mappings");
        }

        @Test
        void preprodUsesIohkTestnetRegistry() {
            // Regression: previously, hardcoded mainnet defaults in the starter's
            // AssetsExtProperties.Cip26 leaked into AssetsExtStoreProperties and
            // shadowed the per-network defaults — preprod ended up cloning
            // cardano-foundation/cardano-token-registry instead of
            // input-output-hk/metadata-registry-testnet.
            Cip26NetworkDefaults defaults = resolve(PREPROD_MAGIC, null);

            assertThat(defaults.isRegistryAvailable()).isTrue();
            assertThat(defaults.getOrganization()).isEqualTo("input-output-hk");
            assertThat(defaults.getProjectName()).isEqualTo("metadata-registry-testnet");
            assertThat(defaults.getMappingsFolder()).isEqualTo("registry");
        }

        @Test
        void previewHasNoRegistryAvailable() {
            // Same regression class as preprod: with leaked mainnet defaults,
            // preview would treat them as a "user override" and clone the
            // mainnet registry on a preview-configured store.
            Cip26NetworkDefaults defaults = resolve(PREVIEW_MAGIC, null);

            assertThat(defaults.isRegistryAvailable()).isFalse();
            assertThat(defaults.getOrganization()).isNull();
            assertThat(defaults.getProjectName()).isNull();
            assertThat(defaults.getMappingsFolder()).isNull();
        }

        @Test
        void sanchonetHasNoRegistryAvailable() {
            Cip26NetworkDefaults defaults = resolve(SANCHONET_MAGIC, null);

            assertThat(defaults.isRegistryAvailable()).isFalse();
            assertThat(defaults.getOrganization()).isNull();
        }

        @Test
        void unknownNetworkHasNoRegistryAvailable() {
            // DevKit / custom magic — no registry unless user explicitly configures one.
            Cip26NetworkDefaults defaults = resolve(DEVKIT_MAGIC, null);

            assertThat(defaults.isRegistryAvailable()).isFalse();
            assertThat(defaults.getOrganization()).isNull();
        }
    }

    @Nested
    @DisplayName("user overrides take priority")
    class UserOverrides {

        @Test
        void overrideWinsOnMainnet() {
            AssetsExtStoreProperties.Cip26 cip26 = new AssetsExtStoreProperties.Cip26();
            cip26.setGitOrganization("custom-org");

            Cip26NetworkDefaults defaults = resolve(MAINNET_MAGIC, cip26);

            assertThat(defaults.getOrganization()).isEqualTo("custom-org");
            // Project / folder fall back to the mainnet defaults since the user
            // didn't override them.
            assertThat(defaults.getProjectName()).isEqualTo("cardano-token-registry");
            assertThat(defaults.getMappingsFolder()).isEqualTo("mappings");
        }

        @Test
        void overrideWinsOnPreprod() {
            AssetsExtStoreProperties.Cip26 cip26 = new AssetsExtStoreProperties.Cip26();
            cip26.setGitOrganization("custom-org");
            cip26.setGitProjectName("custom-repo");
            cip26.setGitMappingsFolder("custom-dir");

            Cip26NetworkDefaults defaults = resolve(PREPROD_MAGIC, cip26);

            assertThat(defaults.getOrganization()).isEqualTo("custom-org");
            assertThat(defaults.getProjectName()).isEqualTo("custom-repo");
            assertThat(defaults.getMappingsFolder()).isEqualTo("custom-dir");
        }

        @Test
        void overrideEnablesRegistryOnPreview() {
            // Operator pointing preview at a custom registry — sync becomes available.
            AssetsExtStoreProperties.Cip26 cip26 = new AssetsExtStoreProperties.Cip26();
            cip26.setGitOrganization("preview-tokens");
            cip26.setGitProjectName("preview-registry");

            Cip26NetworkDefaults defaults = resolve(PREVIEW_MAGIC, cip26);

            assertThat(defaults.isRegistryAvailable()).isTrue();
            assertThat(defaults.getOrganization()).isEqualTo("preview-tokens");
            assertThat(defaults.getProjectName()).isEqualTo("preview-registry");
            // No folder override → fallback default
            assertThat(defaults.getMappingsFolder()).isEqualTo("mappings");
        }

        @Test
        void blankOverrideIsIgnoredAsIfMissing() {
            // Spring Boot relaxed binding can produce empty strings for unset
            // properties — treat them like null so we don't accidentally clone
            // from "https://github.com//.git".
            AssetsExtStoreProperties.Cip26 cip26 = new AssetsExtStoreProperties.Cip26();
            cip26.setGitOrganization("");
            cip26.setGitProjectName("   ");

            Cip26NetworkDefaults defaults = resolve(PREPROD_MAGIC, cip26);

            assertThat(defaults.getOrganization()).isEqualTo("input-output-hk");
            assertThat(defaults.getProjectName()).isEqualTo("metadata-registry-testnet");
        }
    }
}
