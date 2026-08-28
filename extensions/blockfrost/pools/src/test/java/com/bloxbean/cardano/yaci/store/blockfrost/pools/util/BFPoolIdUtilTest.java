package com.bloxbean.cardano.yaci.store.blockfrost.pools.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BFPoolIdUtilTest {

    @Test
    void givenValidHex_shouldReturnSameHex() {
        String hex = "0f292fcaa02b8b2f9b3c8f9fd8e0bb21abedb692a6d5cc1de5f59424";
        assertThat(BFPoolIdUtil.toHex(hex)).isEqualTo(hex);
    }

    @Test
    void givenValidBech32_shouldReturnHex() {
        // Use a known pool1 bech32 and convert to hex and back
        String hex = "0f292fcaa02b8b2f9b3c8f9fd8e0bb21abedb692a6d5cc1de5f59424";
        String bech32 = com.bloxbean.cardano.yaci.store.common.util.PoolUtil.getBech32PoolId(hex);
        assertThat(bech32).startsWith("pool");
        assertThat(BFPoolIdUtil.toHex(bech32)).isEqualTo(hex);
    }

    @Test
    void givenInvalidString_shouldThrowException() {
        assertThatThrownBy(() -> BFPoolIdUtil.toHex("invalid_pool_id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid pool ID format");
    }

    @Test
    void givenNull_shouldThrowException() {
        assertThatThrownBy(() -> BFPoolIdUtil.toHex(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void isValid_givenValidBech32_shouldReturnTrue() {
        String hex = "0f292fcaa02b8b2f9b3c8f9fd8e0bb21abedb692a6d5cc1de5f59424";
        String bech32 = com.bloxbean.cardano.yaci.store.common.util.PoolUtil.getBech32PoolId(hex);
        assertThat(BFPoolIdUtil.isValid(bech32)).isTrue();
    }

    @Test
    void isValid_givenValidHex_shouldReturnTrue() {
        assertThat(BFPoolIdUtil.isValid("0f292fcaa02b8b2f9b3c8f9fd8e0bb21abedb692a6d5cc1de5f59424")).isTrue();
    }

    @Test
    void isValid_givenInvalid_shouldReturnFalse() {
        assertThat(BFPoolIdUtil.isValid("invalid")).isFalse();
    }

    @Test
    void isValid_givenNull_shouldReturnFalse() {
        assertThat(BFPoolIdUtil.isValid(null)).isFalse();
    }

    @Test
    void normalizeOrder_shouldDefaultToAsc() {
        assertThat(BFPoolIdUtil.normalizeOrder(null)).isEqualTo("asc");
        assertThat(BFPoolIdUtil.normalizeOrder("")).isEqualTo("asc");
        assertThat(BFPoolIdUtil.normalizeOrder("invalid")).isEqualTo("asc");
    }

    @Test
    void normalizeOrder_shouldAcceptDesc() {
        assertThat(BFPoolIdUtil.normalizeOrder("desc")).isEqualTo("desc");
        assertThat(BFPoolIdUtil.normalizeOrder("DESC")).isEqualTo("desc");
    }

    @Test
    void normalizeOrder_shouldAcceptAsc() {
        assertThat(BFPoolIdUtil.normalizeOrder("asc")).isEqualTo("asc");
        assertThat(BFPoolIdUtil.normalizeOrder("ASC")).isEqualTo("asc");
    }
}
