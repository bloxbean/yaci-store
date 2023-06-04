package com.bloxbean.cardano.yaci.store.core.util;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;

import java.util.Arrays;

public final class SlotLeaderUtil {
    public static String getByronSlotLeader(String publicKey) {
        byte[] xpub = Arrays.copyOf(HexUtil.decodeHexString(publicKey), 32);
        return HexUtil.encodeHexString(Arrays.copyOf(Blake2bUtil.blake2bHash256(xpub), 28));
    }

    public static String getShelleySlotLeader(String issureVkey) {
        return HexUtil.encodeHexString(Blake2bUtil.blake2bHash224(HexUtil.decodeHexString(issureVkey)));
    }
}
