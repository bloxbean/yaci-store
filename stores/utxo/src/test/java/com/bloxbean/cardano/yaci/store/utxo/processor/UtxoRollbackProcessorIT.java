package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.InvalidTransactionRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.UtxoRepository;
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

    @Autowired
    private InvalidTransactionRepository invalidTransactionRepository;

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

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/invalid_transaction_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteInvalidTransactions() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(13133973, "96bb7918a219dbe0cb01d3962b78a883931da27b5a4987af7c1bd964d7ffc6ff"))
                .currentPoint(new Point(13518703, "5470beb0a38e7793db667269e55ed74b339d35db57e640d8f82de831ee348ba0"))
                .build();

        utxoRollbackProcessor.handleRollbackEvent(rollbackEvent);

        int count = invalidTransactionRepository.findAll().size();
        assertThat(count).isEqualTo(12);
    }

}
