package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CIP-113 programmable token configuration.
 * <p>
 * Beans are always registered. When disabled ({@code store.assets.ext.cip113.enabled=false}),
 * {@link #isEnabled()} returns false and processors/readers skip processing and return empty.
 * <p>
 * Registry NFT policy IDs are maintained per-network in property files:
 * <ul>
 *   <li>{@code cip113/cip113-mainnet.properties}</li>
 *   <li>{@code cip113/cip113-preprod.properties}</li>
 *   <li>{@code cip113/cip113-preview.properties}</li>
 * </ul>
 * The correct file is loaded based on {@code store.cardano.protocol-magic}.
 * Users can override via {@code store.assets.ext.cip113.registry-nft-policy-ids}
 * if needed.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class Cip113Configuration {

    private static final Map<NetworkType, String> NETWORK_PROPERTY_FILES = Map.of(
            NetworkType.MAINNET, "cip113/cip113-mainnet.properties",
            NetworkType.PREPROD, "cip113/cip113-preprod.properties",
            NetworkType.PREVIEW, "cip113/cip113-preview.properties"
    );

    private final StoreProperties storeProperties;
    private final AssetsExtStoreProperties assetsStoreProperties;

    @Getter
    private Set<String> registryNftPolicyIdSet;

    @PostConstruct
    public void init() {
        if (hasUserOverride()) {
            registryNftPolicyIdSet = assetsStoreProperties.getCip113().getRegistryNftPolicyIds().stream()
                    .filter(id -> !id.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            registryNftPolicyIdSet = loadFromPropertyFile();
        }

        NetworkType network = NetworkType.fromProtocolMagic(storeProperties.getProtocolMagic());
        String networkName = network != null ? network.name() : "UNKNOWN (magic=" + storeProperties.getProtocolMagic() + ")";
        log.info("CIP-113 programmable tokens: network={}, enabled={}, policyIds={}",
                networkName, isEnabled(), registryNftPolicyIdSet);
    }

    public boolean isEnabled() {
        return !registryNftPolicyIdSet.isEmpty();
    }

    public boolean isMonitoredPolicyId(String policyId) {
        return registryNftPolicyIdSet.contains(policyId);
    }

    private boolean hasUserOverride() {
        List<String> userPolicyIds = assetsStoreProperties.getCip113().getRegistryNftPolicyIds();
        return userPolicyIds != null && !userPolicyIds.isEmpty()
                && userPolicyIds.stream().anyMatch(id -> !id.isBlank());
    }

    private Set<String> loadFromPropertyFile() {
        NetworkType network = NetworkType.fromProtocolMagic(storeProperties.getProtocolMagic());
        if (network == null) {
            log.warn("Unknown network (protocol-magic={}), CIP-113 will be disabled", storeProperties.getProtocolMagic());
            return Set.of();
        }

        String propertyFile = NETWORK_PROPERTY_FILES.get(network);
        if (propertyFile == null) {
            log.info("No CIP-113 property file for network {}", network);
            return Set.of();
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(propertyFile)) {
            if (is == null) {
                log.warn("CIP-113 property file not found: {}", propertyFile);
                return Set.of();
            }
            Properties props = new Properties();
            props.load(is);
            String ids = props.getProperty("registry-nft-policy-ids", "");
            if (ids.isBlank()) {
                return Set.of();
            }
            return Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            log.warn("Failed to load CIP-113 property file {}: {}", propertyFile, e.getMessage());
            return Set.of();
        }
    }
}
