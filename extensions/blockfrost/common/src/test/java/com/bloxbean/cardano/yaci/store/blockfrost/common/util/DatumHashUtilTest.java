package com.bloxbean.cardano.yaci.store.blockfrost.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatumHashUtilTest {

    // Constr 0 [] and its on-chain datum hash.
    private static final String INLINE_DATUM = "d87980";
    private static final String INLINE_DATUM_HASH = "923918e403bf43c34b4ef6b48eb2ee04babed17320d8d1b9ff9ad086e86f44ec";

    @Test
    void derivesHashFromInlineDatumWhenDataHashMissing() {
        assertThat(DatumHashUtil.resolveDataHash(null, INLINE_DATUM)).isEqualTo(INLINE_DATUM_HASH);
        assertThat(DatumHashUtil.resolveDataHash("", INLINE_DATUM)).isEqualTo(INLINE_DATUM_HASH);
    }

    @Test
    void keepsStoredDataHash() {
        String stored = "0000000000000000000000000000000000000000000000000000000000000001";
        assertThat(DatumHashUtil.resolveDataHash(stored, INLINE_DATUM)).isEqualTo(stored);
    }

    @Test
    void returnsNullWithoutDatum() {
        assertThat(DatumHashUtil.resolveDataHash(null, null)).isNull();
        assertThat(DatumHashUtil.resolveDataHash(null, " ")).isNull();
    }

    @Test
    void returnsNullForUndecodableInlineDatum() {
        assertThat(DatumHashUtil.resolveDataHash(null, "zz")).isNull();
    }
}
