package com.bloxbean.cardano.yaci.store.starter.assetstore;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "store.assets.ext", ignoreUnknownFields = true)
public class AssetsExtProperties {
    private boolean enabled = false;
    private Cip26 cip26 = new Cip26();
    private Cip68 cip68 = new Cip68();
    private Cip113 cip113 = new Cip113();
    private Query query = new Query();

    /**
     * CIP-26 GitHub registry settings.
     *
     * <h3>Why {@link #gitOrganization}, {@link #gitProjectName} and
     * {@link #gitMappingsFolder} must stay {@code null} by default</h3>
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
    public static final class Cip113 {
        // Disabled by default until CIP-113 is officially live on mainnet.
        // Re-enable per deployment via store.assets.ext.cip113.enabled=true.
        private boolean enabled = false;
        /**
         * Comma-separated list of CIP-113 registry NFT policy IDs to monitor.
         * If empty (and no per-network defaults exist), CIP-113 is disabled.
         * If empty but defaults are available, policy IDs are auto-detected from
         * the network's property file.
         */
        private List<String> registryNftPolicyIds = new ArrayList<>();
    }

    @Getter
    @Setter
    public static final class Query {
        private String priority = "CIP_68,CIP_26";
    }
}
