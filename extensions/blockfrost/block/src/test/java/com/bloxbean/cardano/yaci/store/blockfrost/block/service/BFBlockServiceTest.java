package com.bloxbean.cardano.yaci.store.blockfrost.block.service;

import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.BFBlocksStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockRow;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockTxCborRow;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BFBlockServiceTest {

    private static final long BLOCK = 5;

    /** /blocks/{hash_or_number}/txs/cbor reads transaction_cbor, which either CBOR setting fills. */
    @Test
    void blockTxsCborIsServedWhenEitherCborSettingIsOn() {
        assertThat(service(TransactionStoreProperties.builder().saveFullTxCbor(true).build())
                .getBlockTxsCbor(String.valueOf(BLOCK), 0, 10, Order.asc)).hasSize(1);
        assertThat(service(TransactionStoreProperties.builder().saveCbor(true).build())
                .getBlockTxsCbor(String.valueOf(BLOCK), 0, 10, Order.asc)).hasSize(1);
        assertThat(service(TransactionStoreProperties.builder().saveFullTxCbor(true).build())
                .getLatestBlockTxsCbor(0, 10, Order.asc)).hasSize(1);
    }

    @Test
    void blockTxsCborIsNotFoundWithoutEitherSetting() {
        BFBlockService service = service(new TransactionStoreProperties());
        assertThatThrownBy(() -> service.getBlockTxsCbor(String.valueOf(BLOCK), 0, 10, Order.asc))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static BFBlockService service(TransactionStoreProperties transactionProperties) {
        BFBlocksStorageReader reader = mock(BFBlocksStorageReader.class);
        BFBlockRow block = mock(BFBlockRow.class);
        when(block.height()).thenReturn(BLOCK);
        when(reader.findBlockByNumber(BLOCK)).thenReturn(Optional.of(block));
        when(reader.findLatestBlock()).thenReturn(Optional.of(block));
        when(reader.findBlockTxCbor(BLOCK, 0, 10, Order.asc))
                .thenReturn(List.of(new BFBlockTxCborRow("ab".repeat(32), new byte[]{(byte) 0x84}, 0)));

        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("transactionStoreProperties", transactionProperties);
        return new BFBlockService(reader, beans.getBeanProvider(TransactionStoreProperties.class));
    }
}
