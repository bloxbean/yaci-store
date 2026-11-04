package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UtxoTest {

    private static final String TX_HASH = "6d36c0e2f304a5c27b85b3f04e95fc015566d35aef5f061c17c70e3e8b9ee508";
    private static final String BLOCK_HASH = "a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesBlockfrostTxIndexAndBlock() throws Exception {
        Utxo utxo = Utxo.builder()
                .txHash(TX_HASH)
                .outputIndex(3)
                .address("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket")
                .amount(List.of(new Utxo.Amount("lovelace", BigInteger.valueOf(5000000))))
                .block(BLOCK_HASH)
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(utxo));

        assertThat(json.get("output_index").asInt()).isEqualTo(3);
        assertThat(json.get("tx_index").isInt()).isTrue();
        assertThat(json.get("tx_index").asInt()).isEqualTo(3);
        assertThat(json.get("block").asText()).isEqualTo(BLOCK_HASH);
        assertThat(json.get("amount").get(0).get("quantity").asText()).isEqualTo("5000000");
    }

    @Test
    void legacyConstructorStillWorks() {
        Utxo utxo = new Utxo(TX_HASH, 1, "addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket",
                List.of(), null, null, null, 0, 10L, 1000L);

        assertThat(utxo.getOutputIndex()).isEqualTo(1);
        assertThat(utxo.getTxIndex()).isEqualTo(1);
        assertThat(utxo.getBlockTime()).isEqualTo(1000L);
        assertThat(utxo.getBlock()).isNull();
    }

    @Test
    void deserializesWithAndWithoutNewFields() throws Exception {
        String withNewFields = "{\"tx_hash\":\"" + TX_HASH + "\",\"tx_index\":2,\"output_index\":2,\"block\":\"" + BLOCK_HASH + "\"," +
                "\"amount\":[{\"unit\":\"lovelace\",\"quantity\":\"5000000\"}]}";
        String withoutNewFields = "{\"tx_hash\":\"" + TX_HASH + "\",\"output_index\":2," +
                "\"amount\":[{\"unit\":\"lovelace\",\"quantity\":\"5000000\"}]}";

        Utxo utxo = objectMapper.readValue(withNewFields, Utxo.class);
        assertThat(utxo.getOutputIndex()).isEqualTo(2);
        assertThat(utxo.getTxIndex()).isEqualTo(2);
        assertThat(utxo.getBlock()).isEqualTo(BLOCK_HASH);

        Utxo oldUtxo = objectMapper.readValue(withoutNewFields, Utxo.class);
        assertThat(oldUtxo.getOutputIndex()).isEqualTo(2);
        assertThat(oldUtxo.getTxIndex()).isEqualTo(2);
        assertThat(oldUtxo.getBlock()).isNull();
    }
}
