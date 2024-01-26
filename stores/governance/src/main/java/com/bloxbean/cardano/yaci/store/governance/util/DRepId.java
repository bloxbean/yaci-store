package com.bloxbean.cardano.yaci.store.governance.util;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;

//TODO: remove this class once cardano-client-lib is updated
public class DRepId {

    public static String fromKeyHash(String keyHash) {
        String drepId = Bech32.encode(HexUtil.decodeHexString(keyHash), "drep");
        return drepId;
    }

    public static String fromScriptHash(String scriptHash) {
        String drepId = Bech32.encode(HexUtil.decodeHexString(scriptHash), "drep_script");
        return drepId;
    }
}
