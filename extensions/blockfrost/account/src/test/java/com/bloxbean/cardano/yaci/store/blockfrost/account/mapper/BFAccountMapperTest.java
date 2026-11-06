package com.bloxbean.cardano.yaci.store.blockfrost.account.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.AccountInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BFAccountMapperTest {

    @Test
    void toContentDto_unregisteredAccount_keepsBlockfrostNullFields() {
        var info = new AccountInfo("stake_test1upyxd0pgmepw5je3s7tekv0puwx33au8nsaq6y08f9yptecct8flv",
                false, false, null, BigInteger.valueOf(104_728_146),
                BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, null, null);

        JsonNode json = new ObjectMapper().valueToTree(BFAccountMapper.INSTANCE.toContentDto(info));

        // Blockfrost returns these keys with null values for a stake address that was never registered.
        assertThat(json.get("registered").asBoolean()).isFalse();
        assertThat(json.get("active").asBoolean()).isFalse();
        assertThat(json.get("active_epoch").isNull()).isTrue();
        assertThat(json.get("pool_id").isNull()).isTrue();
        assertThat(json.get("drep_id").isNull()).isTrue();
        assertThat(json.get("controlled_amount").asText()).isEqualTo("104728146");
        assertThat(json.get("withdrawable_amount").asText()).isEqualTo("0");
    }
}
