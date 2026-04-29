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

    @Getter
    @Setter
    public static final class Cip26 {
        private boolean enabled = true;
        // Org / project / mappings-folder default to null so Cip26NetworkDefaults
        // can resolve them per protocol-magic. Hardcoding mainnet values here would
        // override that resolution and force every network to clone the mainnet
        // registry — which used to be the case and broke preprod / preview /
        // sanchonet / devkit by silently indexing mainnet data.
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
