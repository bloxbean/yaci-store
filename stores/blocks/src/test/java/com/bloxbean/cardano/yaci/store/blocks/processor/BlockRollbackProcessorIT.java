package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockCborEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockCborRepository;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockRepository;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(properties = "store.blocks.save-cbor=true")
class BlockRollbackProcessorIT {
    @Autowired
    private BlockProcessor blockProcessor;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private BlockCborRepository blockCborRepository;

    @Autowired
    private BlocksStoreProperties blocksStoreProperties;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/blocks_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteBlock() throws Exception {
        blockCborRepository.save(BlockCborEntity.builder()
                .blockHash("d4a58c8cce51691d680c68480013059c6e0713ebf4a42aed4f2a6714a30e256c")
                .slot(44635470L)
                .cborData(new byte[]{0x01, 0x02})
                .cborSize(2)
                .build());
        blockCborRepository.save(BlockCborEntity.builder()
                .blockHash("9d09d4bfec0a67f5a6aea594fc46fc77265b9944ebbbfd9947dfce10de637eb7")
                .slot(44635389L)
                .cborData(new byte[]{0x03})
                .cborSize(1)
                .build());

        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(44635389, "4bbe984de25c79052af653c2424122a7b324b27143886849028789a597ce4ae6"))
                .currentPoint(new Point(44635470, "4bbe984de25c79052af653c2424122a7b324b27143886849028789a597ce4ae6"))
                .build();

        blockProcessor.handleRollbackEvent(rollbackEvent);

        int count = blockRepository.findAll().size();
        assertThat(count).isEqualTo(1);
        assertThat(blocksStoreProperties.isSaveCbor()).isTrue();

        Optional<BlockCborEntity> deletedCbor = blockCborRepository.findById("d4a58c8cce51691d680c68480013059c6e0713ebf4a42aed4f2a6714a30e256c");
        Optional<BlockCborEntity> remainingCbor = blockCborRepository.findById("9d09d4bfec0a67f5a6aea594fc46fc77265b9944ebbbfd9947dfce10de637eb7");
        assertThat(deletedCbor).isEmpty();
        assertThat(remainingCbor).isPresent();
    }
}
