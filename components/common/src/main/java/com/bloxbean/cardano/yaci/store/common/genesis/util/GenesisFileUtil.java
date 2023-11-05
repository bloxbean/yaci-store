package com.bloxbean.cardano.yaci.store.common.genesis.util;

import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;

public class GenesisFileUtil {

    public static String getGenesisfileDefaultFolder(long protocolMagic) {
        var networkFolder = switch (NetworkType.fromProtocolMagic(protocolMagic)) {
            case MAINNET -> "mainnet";
            case PREPROD -> "preprod";
            case PREVIEW -> "preview";
            case SANCHONET -> "sanchonet";
            default -> null;
        };

        return networkFolder;
    }
}
