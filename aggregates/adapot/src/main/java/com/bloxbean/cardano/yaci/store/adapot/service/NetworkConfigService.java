package com.bloxbean.cardano.yaci.store.adapot.service;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.springframework.stereotype.Component;

@Component
public class NetworkConfigService {

    public NetworkConfig getNetworkConfig(int protocolMagic) {
        if(protocolMagic == NetworkConfig.MAINNET_NETWORK_MAGIC) {
            return NetworkConfig.getMainnetConfig();
        } else if(protocolMagic == NetworkConfig.PREPROD_NETWORK_MAGIC) {
            return NetworkConfig.getPreprodConfig();
        } else if(protocolMagic == NetworkConfig.PREVIEW_NETWORK_MAGIC) {
            return NetworkConfig.getPreviewConfig();
        } else if(protocolMagic == NetworkConfig.SANCHONET_NETWORK_MAGIC) {
            return NetworkConfig.getSanchonetConfig();
        } else {
            //TODO: Add custom network config
            return null;
        }
    }
}
