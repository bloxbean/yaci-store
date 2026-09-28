package com.bloxbean.cardano.yaci.store.starter.assetstore;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store.assets.ext", ignoreUnknownFields = true)
public class AssetsExtProperties {
    // Defaults mirror the @ConditionalOnProperty gates so config metadata matches runtime.
    // Blockfrost-extension pattern: the master flag is OFF by default; the sub-flags CIP-26 and
    // CIP-68 default ON, so enabling the master flag alone yields the default behaviour.
    private boolean enabled = false;
    private Cip26 cip26 = new Cip26();
    private Cip68 cip68 = new Cip68();
    private Query query = new Query();

    /**
     * CIP-26 GitHub registry settings.
     *
     * <h2>Why {@link #gitOrganization}, {@link #gitProjectName} and
     * {@link #gitMappingsFolder} must stay {@code null} by default</h2>
     *
     * <p>{@link com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.Cip26NetworkDefaults}
     * resolves these per {@code store.cardano.protocol-magic}: mainnet →
     * {@code cardano-foundation/cardano-token-registry}, preprod →
     * {@code input-output-hk/metadata-registry-testnet}, others → no registry.
     * It only fills in defaults when the user-supplied values are {@code null}
     * (or blank).
     *
     * <p>If we hardcoded mainnet values here, they would flow via
     * {@link AssetsExtAutoConfiguration} into {@code AssetsExtStoreProperties}
     * and shadow the per-network resolution — every network would end up
     * cloning the mainnet registry. That regression was caught on preprod QA
     * (yaci silently indexed 7929 mainnet tokens on a preprod-configured
     * store). See {@code AssetsExtPropertiesTest} for the regression lock.
     */
    @Getter
    @Setter
    public static final class Cip26 {
        // Enabled by default (blockfrost-extension pattern): once the master flag
        // store.assets.ext.enabled is on, CIP-26 sync runs as part of the default behaviour.
        // The git-* fields below stay null so Cip26NetworkDefaults resolves the correct
        // registry per protocol-magic — that per-network resolution, not a default-off flag,
        // is what prevents a preprod node from indexing mainnet metadata.
        private boolean enabled = true;
        @Nullable private String gitOrganization;
        @Nullable private String gitProjectName;
        @Nullable private String gitMappingsFolder;
        private String gitTmpFolder = "/tmp";
        private long syncIntervalMinutes = 60;
        private boolean forceClone = false;
    }

    @Getter
    @Setter
    public static final class Cip68 {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static final class Query {
        private String priority = "CIP_68,CIP_26";
    }
}
