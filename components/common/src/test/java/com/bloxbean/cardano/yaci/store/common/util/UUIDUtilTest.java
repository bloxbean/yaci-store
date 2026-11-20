package com.bloxbean.cardano.yaci.store.common.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDUtilTest {

    @Test
    void nameUUID_sameInput_sameUUID() {
        String txHash = "9163a0795ca1dee7d51f0601f65ec2d90408a653ed89cd45e36c73e04ccdaa8e";

        assertThat(UUIDUtil.nameUUID(txHash, "721")).isEqualTo(UUIDUtil.nameUUID(txHash, "721"));
        assertThat(UUIDUtil.nameUUID(txHash, "721")).isNotEqualTo(UUIDUtil.nameUUID(txHash, "674"));
    }

    @Test
    void nameUUID_isVersion5WithIetfVariant() {
        UUID uuid = UUIDUtil.nameUUID("tx", "1");

        assertThat(uuid.version()).isEqualTo(5);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void nameUUID_differentSplits_differentUUIDs() {
        assertThat(UUIDUtil.nameUUID("ab", "c")).isNotEqualTo(UUIDUtil.nameUUID("a", "bc"));
        assertThat(UUIDUtil.nameUUID("abc")).isNotEqualTo(UUIDUtil.nameUUID("abc", ""));
        assertThat(UUIDUtil.nameUUID("tx", "0", null)).isNotEqualTo(UUIDUtil.nameUUID("tx", "0", "null"));
        assertThat(UUIDUtil.nameUUID("tx", "0", null)).isNotEqualTo(UUIDUtil.nameUUID("tx", "0", ""));
        assertThat(UUIDUtil.nameUUID("tx", "0", null)).isNotEqualTo(UUIDUtil.nameUUID("tx", "0"));
    }
}
