package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StoreUtxoSupplierTest {
    @Test
    void getPageIsUnsupportedForTxEvaluation() {
        StoreUtxoSupplier supplier = new StoreUtxoSupplier(mock(UtxoClient.class), new ReferenceScriptSupplier());

        assertThatThrownBy(() -> supplier.getPage("addr_test1...", 1, 100, OrderEnum.asc))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("getTxOutput");
    }
}
