package com.bloxbean.cardano.yaci.store.blocks.configuration;

import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;

public class BlockConfig {

    private final long mainnetStartTime = 1_506_203_091;
    private final long mainnetByronBlock = 4_490_511;
    private final long testnetStartTime = 1_564_020_236;
    private final long testnetByronBlock = 1_597_133;
    private final int byronProcessTime = 20;
    private final int shellyProcessTime = 1;

    public long getLastByronBlock(long protocolMagic){
        NetworkType networkType = NetworkType.fromProtocolMagic(protocolMagic);

        if (networkType == NetworkType.MAINNET) {
            return mainnetByronBlock;
        } else if (networkType == NetworkType.LEGACY_TESTNET) {
            return testnetByronBlock;
        }

        return 0;
    }

    public long getStartTime(long protocolMagic){
        NetworkType networkType = NetworkType.fromProtocolMagic(protocolMagic);

        if (networkType == NetworkType.MAINNET) {
            return mainnetStartTime;
        } else if (networkType == NetworkType.LEGACY_TESTNET) {
            return testnetStartTime;
        }

        return 0;
    }

    public int getByronProcessTime() {
        return byronProcessTime;
    }

    public int getShellyProcessTime() {
        return shellyProcessTime;
    }
}
