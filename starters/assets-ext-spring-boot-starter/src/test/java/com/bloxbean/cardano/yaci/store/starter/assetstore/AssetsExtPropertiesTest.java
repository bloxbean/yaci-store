package com.bloxbean.cardano.yaci.store.starter.assetstore;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression-lock for {@link AssetsExtProperties}.
 *
 * <p>The CIP-26 git-registry fields ({@code gitOrganization},
 * {@code gitProjectName}, {@code gitMappingsFolder}) MUST default to
 * {@code null} so that
 * {@link com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.Cip26NetworkDefaults}
 * can resolve them per {@code protocol-magic}. Hardcoding mainnet values
 * here was the root cause of an incident where a preprod-configured yaci
 * silently indexed mainnet tokens (because the non-null values shadowed the
 * per-network defaults). See {@code AssetsExtProperties.Cip26} javadoc for
 * the full rationale.
 *
 * <p>If a future change re-introduces non-null defaults for these fields,
 * this test will fail — that's the point. Do <em>not</em> "fix" this test
 * by relaxing the assertions; fix the source.
 */
@DisplayName("AssetsExtProperties")
class AssetsExtPropertiesTest {

    @Test
    @DisplayName("CIP-26 git-* fields default to null so per-network resolution can fire")
    void cip26GitFieldsAreNullByDefault() {
        AssetsExtProperties.Cip26 cip26 = new AssetsExtProperties.Cip26();

        assertThat(cip26.getGitOrganization())
                .as("gitOrganization must be null — Cip26NetworkDefaults resolves it from protocol-magic")
                .isNull();
        assertThat(cip26.getGitProjectName())
                .as("gitProjectName must be null — Cip26NetworkDefaults resolves it from protocol-magic")
                .isNull();
        assertThat(cip26.getGitMappingsFolder())
                .as("gitMappingsFolder must be null — Cip26NetworkDefaults resolves it from protocol-magic")
                .isNull();
    }

    @Test
    @DisplayName("CIP-26 is enabled by default once the master flag is on (blockfrost pattern)")
    void cip26IsEnabledByDefault() {
        // Aligned with the blockfrost extension: sub-flags default on, so enabling the
        // master flag (store.assets.ext.enabled) alone gives the default behaviour. The
        // safeguard against a preprod node indexing mainnet metadata is the per-network
        // git-* resolution (see cip26GitFieldsAreNullByDefault), not a default-off flag.
        assertThat(new AssetsExtProperties.Cip26().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("non-network-specific CIP-26 defaults are unchanged")
    void cip26NonNetworkDefaultsAreSet() {
        AssetsExtProperties.Cip26 cip26 = new AssetsExtProperties.Cip26();

        // Defaults that apply once the operator opts in via cip26.enabled=true.
        assertThat(cip26.getGitTmpFolder()).isEqualTo("/tmp");
        assertThat(cip26.getSyncIntervalMinutes()).isEqualTo(60L);
        assertThat(cip26.isForceClone()).isFalse();
    }

    @Test
    @DisplayName("master extension flag is disabled by default (blockfrost pattern — opt in via one flag)")
    void rootEnabledIsFalseByDefault() {
        // Mirrors the AssetsExtConfiguration @ConditionalOnProperty(matchIfMissing = false)
        // master gate, matching the blockfrost extension (BFAutoConfiguration): the whole
        // extension stays off until an operator sets store.assets.ext.enabled=true, which
        // then brings up CIP-26 + CIP-68 by default.
        assertThat(new AssetsExtProperties().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("CIP-113 is enabled by default, with no policy ID so the bundled per-network registries apply")
    void cip113DefaultsToTheBundledRegistries() {
        // Same reasoning as the CIP-26 git-* fields: a policy ID here would shadow the registry bundled for the
        // network (Cip113Configuration), so a preprod node would follow the mainnet registry.
        AssetsExtProperties.Cip113 cip113 = new AssetsExtProperties.Cip113();

        assertThat(cip113.isEnabled()).isTrue();
        assertThat(cip113.getRegistryNftPolicyIds()).isEmpty();
    }

    @Test
    @DisplayName("the CIP-113 settings reach the module")
    void cip113IsMappedToTheModuleProperties() {
        AssetsExtAutoConfiguration configuration = new AssetsExtAutoConfiguration();
        configuration.properties = new AssetsExtProperties();
        configuration.properties.getCip113().setEnabled(false);
        configuration.properties.getCip113().setRegistryNftPolicyIds(List.of("aa".repeat(28)));

        AssetsExtStoreProperties mapped = configuration.assetsStoreProperties();

        assertThat(mapped.getCip113().isEnabled()).isFalse();
        assertThat(mapped.getCip113().getRegistryNftPolicyIds()).containsExactly("aa".repeat(28));
    }
}
