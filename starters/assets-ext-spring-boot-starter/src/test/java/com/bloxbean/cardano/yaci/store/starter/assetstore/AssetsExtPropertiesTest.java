package com.bloxbean.cardano.yaci.store.starter.assetstore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("non-network-specific CIP-26 defaults are unchanged")
    void cip26NonNetworkDefaultsAreSet() {
        AssetsExtProperties.Cip26 cip26 = new AssetsExtProperties.Cip26();

        // These don't depend on the network and have safe sensible defaults.
        assertThat(cip26.isEnabled()).isTrue();
        assertThat(cip26.getGitTmpFolder()).isEqualTo("/tmp");
        assertThat(cip26.getSyncIntervalMinutes()).isEqualTo(60L);
        assertThat(cip26.isForceClone()).isFalse();
    }

    @Test
    @DisplayName("CIP-113 is disabled by default until officially live on mainnet")
    void cip113IsDisabledByDefault() {
        // Team decision while CIP-113 is still pre-mainnet — see commit history.
        // If this is ever flipped back to true, expect a coordinated yaci-store
        // release that ships matching registry NFT policy IDs.
        assertThat(new AssetsExtProperties.Cip113().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("master extension flag is disabled by default")
    void rootEnabledIsFalseByDefault() {
        // The whole assets-ext extension stays dormant unless an operator
        // opts in via store.assets.ext.enabled=true.
        assertThat(new AssetsExtProperties().isEnabled()).isFalse();
    }
}
