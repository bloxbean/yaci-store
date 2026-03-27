package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves CIP-26 GitHub token registry defaults based on the connected network.
 * <p>
 * If the user explicitly sets git.organization / git.project-name / git.mappings-folder,
 * those values take priority. Otherwise, defaults are derived from {@code store.cardano.protocol-magic}.
 * <p>
 * CIP-26 offchain registries only exist for mainnet and preprod. On other networks (preview,
 * sanchonet, devkit) the sync is automatically disabled unless the user explicitly configures
 * a custom registry.
 *
 * <table>
 *   <caption>CIP-26 registry defaults per network</caption>
 *   <tr><th>Network</th><th>Organization</th><th>Repository</th><th>Mappings Folder</th></tr>
 *   <tr><td>Mainnet</td><td>cardano-foundation</td><td>cardano-token-registry</td><td>mappings</td></tr>
 *   <tr><td>Preprod</td><td>input-output-hk</td><td>metadata-registry-testnet</td><td>registry</td></tr>
 *   <tr><td>Others</td><td colspan="3">No registry available — CIP-26 sync disabled</td></tr>
 * </table>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Cip26NetworkDefaults {

    private final StoreProperties storeProperties;

    @Value("${store.extensions.asset-store.cip26.git.organization:#{null}}")
    private String userOrganization;

    @Value("${store.extensions.asset-store.cip26.git.project-name:#{null}}")
    private String userProjectName;

    @Value("${store.extensions.asset-store.cip26.git.mappings-folder:#{null}}")
    private String userMappingsFolder;

    @Getter
    private String organization;
    @Getter
    private String projectName;
    @Getter
    private String mappingsFolder;
    @Getter
    private boolean registryAvailable;

    @PostConstruct
    void resolve() {
        NetworkType network = NetworkType.fromProtocolMagic(storeProperties.getProtocolMagic());
        boolean hasUserOverride = userOrganization != null || userProjectName != null;

        if (network == NetworkType.MAINNET) {
            organization = defaultIfNull(userOrganization, "cardano-foundation");
            projectName = defaultIfNull(userProjectName, "cardano-token-registry");
            mappingsFolder = defaultIfNull(userMappingsFolder, "mappings");
            registryAvailable = true;
        } else if (network == NetworkType.PREPROD) {
            organization = defaultIfNull(userOrganization, "input-output-hk");
            projectName = defaultIfNull(userProjectName, "metadata-registry-testnet");
            mappingsFolder = defaultIfNull(userMappingsFolder, "registry");
            registryAvailable = true;
        } else if (hasUserOverride) {
            // User explicitly configured a custom registry for this network
            organization = userOrganization;
            projectName = userProjectName;
            mappingsFolder = defaultIfNull(userMappingsFolder, "mappings");
            registryAvailable = true;
        } else {
            // Preview, Sanchonet, DevKit, etc. — no known CIP-26 registry
            organization = null;
            projectName = null;
            mappingsFolder = null;
            registryAvailable = false;
        }

        String networkName = network != null ? network.name() : "UNKNOWN (magic=" + storeProperties.getProtocolMagic() + ")";
        if (registryAvailable) {
            log.info("CIP-26 token registry: network={}, org={}, repo={}, folder={}",
                    networkName, organization, projectName, mappingsFolder);
        } else {
            log.info("CIP-26 token registry: no registry available for network={}, sync will be skipped",
                    networkName);
        }
    }

    private static String defaultIfNull(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
