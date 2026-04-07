package com.bloxbean.cardano.yaci.store.starter.assetstore;

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
        private boolean enabled = false;
        private String gitOrganization = "cardano-foundation";
        private String gitProjectName = "cardano-token-registry";
        private String gitMappingsFolder = "mappings";
        private String gitTmpFolder = "/tmp";
        private long syncIntervalMinutes = 60;
    }

    @Getter
    @Setter
    public static final class Cip68 {
        private boolean enabled = true;
        private boolean fungibleEnabled = true;
        private boolean nftEnabled = false;
    }

    @Getter
    @Setter
    public static final class Cip113 {
        private boolean enabled = false;
        /**
         * Comma-separated list of CIP-113 registry NFT policy IDs to monitor.
         * If empty, policy IDs are auto-detected from the network's property file.
         */
        private List<String> registryNftPolicyIds = new ArrayList<>();
    }

    @Getter
    @Setter
    public static final class Query {
        private String priority = "CIP_68,CIP_26";
    }
}
