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
}
