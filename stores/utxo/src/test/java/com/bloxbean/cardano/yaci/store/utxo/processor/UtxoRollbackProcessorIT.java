package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
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
                .rollbackTo(new Point(44185446, "925347abf637eb2d436349b78589bb257e396c0a4cc133236b76e56ffebc57bb"))
                .currentPoint(new Point(44185802, "64069e4f2351d25a572189c0df03f2c9e0a1200b9fe897cc5fb74106ed6ed6ad"))
                .build();

        utxoRollbackProcessor.handleRollbackEvent(rollbackEvent);

        int count = utxoRepository.findAll().size();
        assertThat(count).isEqualTo(14);
    }

}
