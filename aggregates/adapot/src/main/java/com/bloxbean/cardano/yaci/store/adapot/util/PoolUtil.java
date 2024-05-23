package com.bloxbean.cardano.yaci.store.adapot.util;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;

public class PoolUtil {
    private static final String POOL_ID_PREFIX = "pool";

    public static String getPoolId(String poolKeyHash) {
        return Bech32.encode(HexUtil.decodeHexString(poolKeyHash), POOL_ID_PREFIX);
    }

}
