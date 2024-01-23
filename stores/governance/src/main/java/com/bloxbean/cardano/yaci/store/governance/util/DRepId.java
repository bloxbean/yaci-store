package com.bloxbean.cardano.yaci.store.governance.util;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;

//TODO: remove this class once cardano-client-lib is updated
public class DRepId {
    public static final String DREP_ID_PREFIX = "drep";

    public static String fromKeyHash(String keyHash) {
        String drepId = Bech32.encode(HexUtil.decodeHexString(keyHash), DREP_ID_PREFIX);
        return drepId;
    }
}
