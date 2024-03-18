package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.JpaBlockRepository;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BlockRollbackProcessorIT {
    @Autowired
    private BlockProcessor blockProcessor;

    @Autowired
    private JpaBlockRepository jpaBlockRepository;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/blocks_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteBlock() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(44635389, "4bbe984de25c79052af653c2424122a7b324b27143886849028789a597ce4ae6"))
                .currentPoint(new Point(44635470, "4bbe984de25c79052af653c2424122a7b324b27143886849028789a597ce4ae6"))
                .build();

        blockProcessor.handleRollbackEvent(rollbackEvent);

        int count = jpaBlockRepository.findAll().size();
        assertThat(count).isEqualTo(1);
    }
}
