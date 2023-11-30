package com.bloxbean.cardano.yaci.store.metadata.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.repository.TxMetadataLabelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
class MetadataRollbackProcessorIT {
    @Autowired
    private MetadataRollbackProcessor metadataRollbackProcessor;

    @Autowired
    private TxMetadataLabelRepository metadataLabelRepository;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/metadata_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteTransactionMetadata() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(10620521, "8a37b9c53b0d1308dc30a90da83f08c565359955e8e4c1443040a9b4e0c8f29b"))
                .currentPoint(new Point(10623665, "8913504640f622541ea39b34e681cb3cca05c5e30115ecd7a6a2c018fbe2d4f9"))
                .build();

        metadataRollbackProcessor.handleRollbackEvent(rollbackEvent);

        int count = metadataLabelRepository.findAll().size();
        assertThat(count).isEqualTo(4);
    }
}
