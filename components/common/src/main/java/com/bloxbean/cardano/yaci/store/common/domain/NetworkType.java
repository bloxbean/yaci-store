package com.bloxbean.cardano.yaci.store.common.domain;

public enum NetworkType {
    MAINNET(764824073),
    LEGACY_TESTNET(1097911063),
    PREPROD(1),
    PREVIEW(2),
    SANCHONET(4);

    long protocolMagic;
    NetworkType(long protocolMagic) {
        this.protocolMagic = protocolMagic;
    }

    public static NetworkType fromProtocolMagic(long protocolMagic) {
        for (NetworkType networkType : values()) {
            if (networkType.getProtocolMagic() == protocolMagic) {
                return networkType;
            }
        }
        return null;
    }

    public long getProtocolMagic() {
        return protocolMagic;
    }
}
