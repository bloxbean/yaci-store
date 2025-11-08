package com.bloxbean.cardano.yaci.store.blocks.storage.impl;

import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockCbor;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockCborStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockCborStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockCborRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@DataJpaTest
@Sql(
        scripts = {
                "classpath:db/store/h2/V0_100_1__init.sql",
                "classpath:db/store/h2/V0_100_2__add_block_cbor_table.sql"
        },
        executionPhase = BEFORE_TEST_CLASS
)
class BlockCborStorageTest {

    @Autowired
    private BlockCborRepository blockCborRepository;

    private BlockCborStorage blockCborStorage;
    private BlockCborStorageReader blockCborStorageReader;

    private BlocksStoreProperties properties;

    @BeforeEach
    void setUp() {
        properties = BlocksStoreProperties.builder()
                .saveCbor(true)
                .build();

        blockCborStorage = new BlockCborStorageImpl(
                blockCborRepository,
                properties
        );

        blockCborStorageReader = new BlockCborStorageReaderImpl(blockCborRepository);
    }

    @Test
    void shouldSaveCborData() {
        byte[] cborData = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        BlockCbor blockCbor = BlockCbor.builder()
                .blockHash("block123abc")
                .cborData(cborData)
                .cborSize(cborData.length)
                .slot(54321L)
                .build();

        blockCborStorage.save(blockCbor);

        Optional<BlockCbor> retrieved = blockCborStorageReader.getBlockCborByHash("block123abc");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getBlockHash()).isEqualTo("block123abc");
        assertThat(retrieved.get().getCborData()).isEqualTo(cborData);
        assertThat(retrieved.get().getCborSize()).isEqualTo(5);
        assertThat(retrieved.get().getSlot()).isEqualTo(54321L);
    }

    @Test
    void shouldCheckCborExists() {
        BlockCbor blockCbor = BlockCbor.builder()
                .blockHash("existsblock")
                .cborData(new byte[]{0x0A, 0x0B})
                .cborSize(2)
                .slot(999L)
                .build();

        blockCborStorage.save(blockCbor);

        assertThat(blockCborStorageReader.cborExists("existsblock")).isTrue();
        assertThat(blockCborStorageReader.cborExists("nonexistent")).isFalse();
    }

    @Test
    void shouldDeleteCborBySlotGreaterThan() {
        BlockCbor cbor1 = BlockCbor.builder()
                .blockHash("block1")
                .cborData(new byte[]{0x01})
                .cborSize(1)
                .slot(100L)
                .build();

        BlockCbor cbor2 = BlockCbor.builder()
                .blockHash("block2")
                .cborData(new byte[]{0x02})
                .cborSize(1)
                .slot(300L)
                .build();

        blockCborStorage.save(cbor1);
        blockCborStorage.save(cbor2);

        int deleted = blockCborStorage.deleteBySlotGreaterThan(200L);

        assertThat(deleted).isEqualTo(1);
        assertThat(blockCborStorageReader.cborExists("block1")).isTrue();
        assertThat(blockCborStorageReader.cborExists("block2")).isFalse();
    }

    @Test
    void shouldNotSaveEmptyCborData() {
        BlockCbor emptyCbor = BlockCbor.builder()
                .blockHash("emptyblock")
                .cborData(null)
                .slot(100L)
                .build();

        blockCborStorage.save(emptyCbor);

        assertThat(blockCborStorageReader.cborExists("emptyblock")).isFalse();
    }

    @Test
    void shouldHandleLargeCborData() {
        byte[] largeCborData = new byte[1024 * 100];
        for (int i = 0; i < largeCborData.length; i++) {
            largeCborData[i] = (byte) (i % 256);
        }

        BlockCbor largeCbor = BlockCbor.builder()
                .blockHash("largeblock")
                .cborData(largeCborData)
                .cborSize(largeCborData.length)
                .slot(5000L)
                .build();

        blockCborStorage.save(largeCbor);

        Optional<BlockCbor> retrieved = blockCborStorageReader.getBlockCborByHash("largeblock");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCborSize()).isEqualTo(1024 * 100);
        assertThat(retrieved.get().getCborData()).hasSize(1024 * 100);
    }
}

