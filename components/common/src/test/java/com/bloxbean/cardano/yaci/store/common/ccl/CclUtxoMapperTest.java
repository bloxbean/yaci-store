package com.bloxbean.cardano.yaci.store.common.ccl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CclUtxoMapperTest {

    @Test
    void fromAddressUtxoMapsLovelaceAssetsDatumAndReferenceScriptHash() {
        AddressUtxo addressUtxo = AddressUtxo.builder()
                .txHash("tx1")
                .outputIndex(2)
                .ownerAddr("addr_test1...")
                .lovelaceAmount(BigInteger.valueOf(5_000_000))
                .amounts(List.of(Amt.builder()
                        .unit("policyasset")
                        .quantity(BigInteger.valueOf(7))
                        .build()))
                .dataHash("datum-hash")
                .inlineDatum("d87980")
                .referenceScriptHash("script-hash")
                .build();

        var cclUtxo = CclUtxoMapper.fromAddressUtxo(addressUtxo);

        assertThat(cclUtxo.getTxHash()).isEqualTo("tx1");
        assertThat(cclUtxo.getOutputIndex()).isEqualTo(2);
        assertThat(cclUtxo.getAddress()).isEqualTo("addr_test1...");
        assertThat(cclUtxo.getDataHash()).isEqualTo("datum-hash");
        assertThat(cclUtxo.getInlineDatum()).isEqualTo("d87980");
        assertThat(cclUtxo.getReferenceScriptHash()).isEqualTo("script-hash");
        assertThat(cclUtxo.getAmount())
                .anySatisfy(amount -> {
                    assertThat(amount.getUnit()).isEqualTo("lovelace");
                    assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(5_000_000));
                })
                .anySatisfy(amount -> {
                    assertThat(amount.getUnit()).isEqualTo("policyasset");
                    assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(7));
                });
    }

    @Test
    void fromStoreUtxoMapsApiUtxoShape() {
        Utxo storeUtxo = Utxo.builder()
                .txHash("tx2")
                .outputIndex(1)
                .address("addr_test1...")
                .amount(List.of(Utxo.Amount.builder()
                        .unit("lovelace")
                        .quantity(BigInteger.valueOf(2_000_000))
                        .build()))
                .dataHash("datum-hash")
                .inlineDatum("d87980")
                .referenceScriptHash("script-hash")
                .build();

        var cclUtxo = CclUtxoMapper.fromStoreUtxo(storeUtxo);

        assertThat(cclUtxo.getTxHash()).isEqualTo("tx2");
        assertThat(cclUtxo.getOutputIndex()).isEqualTo(1);
        assertThat(cclUtxo.getAddress()).isEqualTo("addr_test1...");
        assertThat(cclUtxo.getDataHash()).isEqualTo("datum-hash");
        assertThat(cclUtxo.getInlineDatum()).isEqualTo("d87980");
        assertThat(cclUtxo.getReferenceScriptHash()).isEqualTo("script-hash");
        assertThat(cclUtxo.getAmount()).singleElement().satisfies(amount -> {
            assertThat(amount.getUnit()).isEqualTo("lovelace");
            assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(2_000_000));
        });
    }
}
