package com.bloxbean.carano.yaci.indexer.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
