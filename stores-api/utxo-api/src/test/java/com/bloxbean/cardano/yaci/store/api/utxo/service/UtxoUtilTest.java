package com.bloxbean.cardano.yaci.store.api.utxo.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UtxoUtilTest {

    @Test
    void mapsBlockHashAndUsesOutputIndexForTxIndex() {
        AddressUtxo addressUtxo = AddressUtxo.builder()
                .txHash("6d36c0e2f304a5c27b85b3f04e95fc015566d35aef5f061c17c70e3e8b9ee508")
                .outputIndex(1)
                .txIndex(7) //index of the tx within its block, must not leak into tx_index
                .blockHash("a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1")
                .blockNumber(100L)
                .blockTime(1790592614L)
                .ownerAddr("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket")
                .amounts(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(5000000)).build()))
                .build();

        Utxo utxo = UtxoUtil.addressUtxoToUtxo(addressUtxo);

        assertThat(utxo.getBlock()).isEqualTo("a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1d4f6e8c0a2b1");
        assertThat(utxo.getOutputIndex()).isEqualTo(1);
        assertThat(utxo.getTxIndex()).isEqualTo(1);
        assertThat(utxo.getBlockNumber()).isEqualTo(100L);
    }

    @Test
    void genesisUtxoKeepsGenesisBlockHash() {
        AddressUtxo addressUtxo = AddressUtxo.builder()
                .txHash("6d36c0e2f304a5c27b85b3f04e95fc015566d35aef5f061c17c70e3e8b9ee508")
                .outputIndex(0)
                .blockHash("Genesis")
                .blockNumber(-1L)
                .ownerAddr("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket")
                .amounts(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(5000000)).build()))
                .build();

        Utxo utxo = UtxoUtil.addressUtxoToUtxo(addressUtxo);

        assertThat(utxo.getBlock()).isEqualTo("Genesis");
        assertThat(utxo.getTxIndex()).isZero();
    }

    @Test
    void addressUtxoToUtxo_hashesOriginalBytesOfNonCanonicalInlineDatum() {
        // Inline datum of preprod tx 48547c528e3fc501ebf59ed0a930cd4688698c0e5ad03778ae01a12d6e71d1e9#0.
        // Its map keys are not in canonical order, so a decode + re-encode gives different bytes and a different hash.
        String inlineDatum = "d8799fd8799fd8799f581c382771cf2cae02f1e461306f41cf4f71a19a635b73ead12a9d63703affd8799fd8799fd8799f581c1d"
                + "a499e672e3b5cdbb6ff95954f772902dd096df2abd6ab37df8f0afffffffffa546736f632e7974582b68747470733a2f2f777777"
                + "2e796f75747562652e636f6d2f77617463683f763d5668685f476542504f687348736f632e747774724d616461646f6d61696e73"
                + "5f696f4b736f632e6473632e696e76581c687474703a2f2f646973636f72642e67672f534450753943536539434b736f632e6473"
                + "632e7573724762656172706177487765622e68747470581a68747470733a2f2f7777772e616461646f6d61696e732e696f2f80ff";

        AddressUtxo addressUtxo = AddressUtxo.builder()
                .txHash("48547c528e3fc501ebf59ed0a930cd4688698c0e5ad03778ae01a12d6e71d1e9")
                .outputIndex(0)
                .amounts(List.of())
                .inlineDatum(inlineDatum)
                .build();

        assertThat(UtxoUtil.addressUtxoToUtxo(addressUtxo).getDataHash())
                .isEqualTo("58916976ce8cf944de716387258c6046f8c5d6b4bb950d6a96acefdfcc921458");
    }
}
