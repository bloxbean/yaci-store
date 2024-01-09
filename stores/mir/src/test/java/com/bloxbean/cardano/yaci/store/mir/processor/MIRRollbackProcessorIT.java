package com.bloxbean.cardano.yaci.store.mir.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.MIRRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
class MIRRollbackProcessorIT {
    @Autowired
    private MIRProcessor mirProcessor;

    @Autowired
    private MIRRepository mirRepository;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/mir_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteMirRecords() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(950410, "b9ebe459c3ba8e890f951dacb50cba6fa02cf099c6308c7abd26cf616bf26ca5"))
                .currentPoint(new Point(13220525, "c46dba8ba6246f6f3b5b7cca258271406c9a2d119baf3ebc383ad6eed63f33e1"))
                .build();

        mirProcessor.handleRollbackEvent(rollbackEvent);

        int count = mirRepository.findAll().size();
        assertThat(count).isEqualTo(1);
    }
}
