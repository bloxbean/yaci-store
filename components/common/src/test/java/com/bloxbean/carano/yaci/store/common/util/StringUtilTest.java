package com.bloxbean.carano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class StringUtilTest {

    @Test
    void isEmpty_whenNull() {
        String s = null;

        assertThat(StringUtil.isEmpty(s)).isTrue();
    }

    @Test
    void isEmpty_whenEmpty() {
        String s = "";

        assertThat(StringUtil.isEmpty(s)).isTrue();
    }

    @Test
    void isEmpty_whenNonEmpty() {
        String s = "abcd";

        assertThat(StringUtil.isEmpty(s)).isFalse();
    }
}
