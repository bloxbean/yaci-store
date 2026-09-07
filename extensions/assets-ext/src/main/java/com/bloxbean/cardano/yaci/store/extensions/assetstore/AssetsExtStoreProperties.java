package com.bloxbean.cardano.yaci.store.extensions.assetstore;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class AssetsExtStoreProperties {

    @Builder.Default
    private Cip26 cip26 = new Cip26();

    @Builder.Default
    private String defaultQueryPriority = "CIP_68,CIP_26";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Cip26 {
        // Disabled by default — the off-chain GitHub registry is a separate
        // trust source from the chain itself. Projects that want it must opt
        // in explicitly via store.assets.ext.cip26.enabled=true.
        @Builder.Default
        private boolean enabled = false;

        // Null means "let Cip26NetworkDefaults pick the per-network default".
        @Nullable
        @Builder.Default
        private String gitOrganization = null;

        @Nullable
        @Builder.Default
        private String gitProjectName = null;

        @Nullable
        @Builder.Default
        private String gitMappingsFolder = null;

        @Builder.Default
        private String gitTmpFolder = "/tmp";

        @Builder.Default
        private boolean forceClone = false;
    }
}
