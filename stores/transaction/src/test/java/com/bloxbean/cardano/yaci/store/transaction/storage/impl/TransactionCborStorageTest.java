package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxnCbor;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnCborRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@DataJpaTest
@Sql(
        scripts = {
                "classpath:db/store/h2/V0_300_1__init.sql",
                "classpath:db/store/h2/V0_300_3__add_transaction_cbor_table.sql"
        },
        executionPhase = BEFORE_TEST_CLASS
)
class TransactionCborStorageTest {

    @Autowired
    private TxnCborRepository txnCborRepository;

    private TransactionCborStorage transactionCborStorage;
    private TransactionCborStorageReader transactionCborStorageReader;

    @BeforeEach
    void setUp() {
        transactionCborStorage = new TransactionCborStorageImpl(txnCborRepository);
        transactionCborStorageReader = new TransactionCborStorageReaderImpl(txnCborRepository);
    }

    @Test
    void shouldSaveCborData() {
        byte[] cborData = new byte[]{0x01, 0x02, 0x03, 0x04};
        TxnCbor txnCbor = TxnCbor.builder()
                .txHash("abc123def456")
                .cborData(cborData)
                .cborSize(cborData.length)
                .slot(12345L)
                .build();

        transactionCborStorage.save(List.of(txnCbor));

        Optional<TxnCbor> retrieved = transactionCborStorageReader.getTxCborByHash("abc123def456");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTxHash()).isEqualTo("abc123def456");
        assertThat(retrieved.get().getCborData()).isEqualTo(cborData);
        assertThat(retrieved.get().getCborSize()).isEqualTo(4);
        assertThat(retrieved.get().getSlot()).isEqualTo(12345L);
    }

    @Test
    void shouldCheckCborExists() {
        TxnCbor txnCbor = TxnCbor.builder()
                .txHash("existstest123")
                .cborData(new byte[]{0x01, 0x02})
                .cborSize(2)
                .slot(100L)
                .build();

        transactionCborStorage.save(List.of(txnCbor));

        assertThat(transactionCborStorageReader.cborExists("existstest123")).isTrue();
        assertThat(transactionCborStorageReader.cborExists("nonexistent")).isFalse();
    }

    @Test
    void shouldDeleteCborBySlotGreaterThan() {
        TxnCbor cbor1 = TxnCbor.builder()
                .txHash("tx1")
                .cborData(new byte[]{0x01})
                .cborSize(1)
                .slot(100L)
                .build();

        TxnCbor cbor2 = TxnCbor.builder()
                .txHash("tx2")
                .cborData(new byte[]{0x02})
                .cborSize(1)
                .slot(200L)
                .build();

        transactionCborStorage.save(List.of(cbor1, cbor2));

        int deleted = transactionCborStorage.deleteBySlotGreaterThan(150L);

        assertThat(deleted).isEqualTo(1);
        assertThat(transactionCborStorageReader.cborExists("tx1")).isTrue();
        assertThat(transactionCborStorageReader.cborExists("tx2")).isFalse();
    }

    @Test
    void shouldNotSaveEmptyCborData() {
        TxnCbor emptyCbor = TxnCbor.builder()
                .txHash("empty123")
                .cborData(null)
                .slot(100L)
                .build();

        transactionCborStorage.save(List.of(emptyCbor));

        assertThat(transactionCborStorageReader.cborExists("empty123")).isFalse();
    }

    @Test
    void shouldSaveMultipleCborRecords() {
        List<TxnCbor> cborList = List.of(
                TxnCbor.builder().txHash("tx1").cborData(new byte[]{0x01}).cborSize(1).slot(100L).build(),
                TxnCbor.builder().txHash("tx2").cborData(new byte[]{0x02}).cborSize(1).slot(101L).build(),
                TxnCbor.builder().txHash("tx3").cborData(new byte[]{0x03}).cborSize(1).slot(102L).build()
        );

        transactionCborStorage.save(cborList);

        assertThat(transactionCborStorageReader.cborExists("tx1")).isTrue();
        assertThat(transactionCborStorageReader.cborExists("tx2")).isTrue();
        assertThat(transactionCborStorageReader.cborExists("tx3")).isTrue();
    }
}

