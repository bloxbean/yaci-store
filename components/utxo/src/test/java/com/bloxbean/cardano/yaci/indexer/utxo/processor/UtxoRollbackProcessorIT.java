package com.bloxbean.cardano.yaci.indexer.utxo.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.indexer.events.RollbackEvent;
import com.bloxbean.cardano.yaci.indexer.utxo.repository.UtxoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
public class UtxoRollbackProcessorIT {

    @Autowired
    private UtxoRollbackProcessor utxoRollbackProcessor;

    @Autowired
    private UtxoRepository utxoRepository;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/address_utxo_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteAddressUtxos() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(14486302, "5068285d5eccc63bbfe0caa446fde9c28d77e45b6f34d37c29bbae8d47c30b9b"))
                .currentPoint(new Point(14487479, "5ca2e98fe743c4dc92b323a6cd244825e663aa1e35fd3123487c8c0a170196e2"))
                .build();

        utxoRollbackProcessor.handleRollbackEvent(rollbackEvent);

        int count = utxoRepository.findAll().size();
        assertThat(count).isEqualTo(13);
    }

    //TODO -- Add tests for invalid transaction rollback
}
