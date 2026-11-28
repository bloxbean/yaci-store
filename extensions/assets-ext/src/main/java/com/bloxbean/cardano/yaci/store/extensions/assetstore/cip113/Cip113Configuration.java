package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The CIP-113 registries the module follows (ADR 0011).
 * <p>
 * {@code store.assets.ext.cip113.enabled} switches CIP-113 on or off. When it is on, the policy IDs of the registries
 * deployed on the network are read from the file bundled for it, picked by {@code store.cardano.protocol-magic}:
 * <ul>
 *   <li>{@code cip113/cip113-mainnet.properties}</li>
 *   <li>{@code cip113/cip113-preprod.properties}</li>
 *   <li>{@code cip113/cip113-preview.properties}</li>
 * </ul>
 * {@code store.assets.ext.cip113.registry-nft-policy-ids} replaces them, for another registry or a private network.
 * Without any policy ID (CIP-113 off, or a network without a known registry) no registry node is read or stored and
 * every token is served as {@code NATIVE}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Cip113Configuration {

    private static final Map<NetworkType, String> NETWORK_PROPERTY_FILES = Map.of(
            NetworkType.MAINNET, "cip113/cip113-mainnet.properties",
            NetworkType.PREPROD, "cip113/cip113-preprod.properties",
            NetworkType.PREVIEW, "cip113/cip113-preview.properties"
    );

    static final String POLICY_IDS_PROPERTY = "registry-nft-policy-ids";

    private final StoreProperties storeProperties;
    private final AssetsExtStoreProperties assetsStoreProperties;

    /** The policy IDs of the registry NFTs, in lower case; empty when CIP-113 is off. */
    @Getter
    private Set<String> registryNftPolicyIds = Set.of();

    @PostConstruct
    void resolve() {
        if (!assetsStoreProperties.getCip113().isEnabled()) {
            log.info("CIP-113 programmable tokens: disabled (store.assets.ext.cip113.enabled=false)");
            return;
        }

        NetworkType network = NetworkType.fromProtocolMagic(storeProperties.getProtocolMagic());
        String networkName = network != null ? network.name() : "UNKNOWN (magic=" + storeProperties.getProtocolMagic() + ")";
        List<String> userPolicyIds = assetsStoreProperties.getCip113().getRegistryNftPolicyIds();
        boolean userOverride = userPolicyIds != null && userPolicyIds.stream().anyMatch(id -> id != null && !id.isBlank());

        registryNftPolicyIds = userOverride ? normalize(userPolicyIds.stream()) : loadBundled(network);

        if (registryNftPolicyIds.isEmpty()) {
            log.info("CIP-113 programmable tokens: no registry known for network={}, every token is served as NATIVE",
                    networkName);
        } else {
            log.info("CIP-113 programmable tokens: network={}, registry NFT policy IDs={}{}", networkName,
                    registryNftPolicyIds, userOverride ? " (store.assets.ext.cip113.registry-nft-policy-ids)" : "");
        }
    }

    public boolean isEnabled() {
        return !registryNftPolicyIds.isEmpty();
    }

    public boolean isRegistryPolicyId(String policyId) {
        return policyId != null && registryNftPolicyIds.contains(policyId.toLowerCase(Locale.ROOT));
    }

    private Set<String> loadBundled(NetworkType network) {
        String file = network != null ? NETWORK_PROPERTY_FILES.get(network) : null;
        if (file == null) {
            return Set.of();
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(file)) {
            if (in == null) {
                log.warn("CIP-113 registry file {} is missing from the classpath", file);
                return Set.of();
            }
            Properties properties = new Properties();
            properties.load(in);
            return normalize(Arrays.stream(properties.getProperty(POLICY_IDS_PROPERTY, "").split(",")));
        } catch (IOException e) {
            log.warn("Could not read the CIP-113 registry file {}: {}", file, e.getMessage());
            return Set.of();
        }
    }

    private static Set<String> normalize(Stream<String> policyIds) {
        return policyIds.filter(id -> id != null && !id.isBlank())
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
