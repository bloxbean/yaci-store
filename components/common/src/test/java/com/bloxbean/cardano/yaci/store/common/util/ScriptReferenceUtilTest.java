package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.util.HexUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptReferenceUtilTest {

    // [0, all [sig 34cfb64a..., sig 9d34e325...]] with the sub-script array encoded indefinite (9f ... ff)
    private static final String NATIVE_INDEFINITE_SCRIPT_REF =
            "820082019f8200581c34cfb64a68971628d0918e0c2892d726c21ccd8e3274641055335fda8200581c9d34e325167575e685bce15a047397cdc859477d00d9d710811b0707ff";
    // Same script with a definite-length sub-script array (82)
    private static final String NATIVE_DEFINITE_SCRIPT_REF =
            "82008201828200581c34cfb64a68971628d0918e0c2892d726c21ccd8e3274641055335fda8200581c9d34e325167575e685bce15a047397cdc859477d00d9d710811b0707";

    @Test
    void nativeScriptHash_isComputedFromTheOriginalBytes() throws Exception {
        // blake2b-224(0x00 || original script bytes)
        assertThat(ScriptReferenceUtil.getReferenceScriptHash(HexUtil.decodeHexString(NATIVE_INDEFINITE_SCRIPT_REF)))
                .isEqualTo("495573862056785611a39ef13b9dca637cbf239807b239e10fce11f0");
    }

    @Test
    void nativeScriptHash_definiteEncoding_matchesCclReEncodedHash() throws Exception {
        byte[] scriptRef = HexUtil.decodeHexString(NATIVE_DEFINITE_SCRIPT_REF);
        String cclHash = HexUtil.encodeHexString(ScriptReferenceUtil.deserializeScriptRef(scriptRef).getScriptHash());

        assertThat(ScriptReferenceUtil.getReferenceScriptHash(scriptRef))
                .isEqualTo("081e4a9e46acaa8c0ff0d4b6a7a03ac4732eaf3ee40d32ac092f3b80")
                .isEqualTo(cclHash);
    }
}
