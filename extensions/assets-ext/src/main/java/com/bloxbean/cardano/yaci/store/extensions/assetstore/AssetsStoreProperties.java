package com.bloxbean.cardano.yaci.store.extensions.assetstore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain properties POJO for the assets-ext extension module.
 * <p>
 * Bean is created by the starter's AutoConfiguration, which maps
 * {@code store.assets.ext.*} Spring Boot properties into this object.
 * This follows the yaci-store convention of keeping {@code @Value} annotations
 * out of extension modules.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssetsStoreProperties {

    @Builder.Default
    private Cip26 cip26 = new Cip26();

    @Builder.Default
    private Cip113 cip113 = new Cip113();

    @Builder.Default
    private String defaultQueryPriority = "CIP_68,CIP_26";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Cip26 {
        @Builder.Default
        private boolean enabled = false;

        @Builder.Default
        private String gitOrganization = null;

        @Builder.Default
        private String gitProjectName = null;

        @Builder.Default
        private String gitMappingsFolder = null;

        @Builder.Default
        private String gitTmpFolder = "/tmp";

        @Builder.Default
        private boolean forceClone = false;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Cip113 {
        @Builder.Default
        private List<String> registryNftPolicyIds = new ArrayList<>();
    }
}
