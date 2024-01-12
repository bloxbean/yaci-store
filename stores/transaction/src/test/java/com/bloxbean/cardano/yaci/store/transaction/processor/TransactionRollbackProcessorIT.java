package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnEntityRepository;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnWitnessRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
class TransactionRollbackProcessorIT {

    @Autowired
    private TxnEntityRepository txnEntityRepository;

    @Autowired
    private TxnWitnessRepository txnWitnessRepository;

    @Autowired
    private TransactionRollbackProcessor transactionRollbackProcessor;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/transaction_data.sql", executionPhase = BEFORE_TEST_METHOD),
            @Sql(value = "classpath:scripts/transaction_witness_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void handleRollBackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(10811083, "dbcec01a23519abe2c308f7ad72ec9b25a0f725bf7cccb76b1ca0defd6f19f7b"))
                .currentPoint(new Point(10811653, "e43b09f3d95f83aadc4d7436c5c9067ff04ab876b6ed8acbbed1c0ab49743869"))
                .build();

        transactionRollbackProcessor.handleRollbackEvent(rollbackEvent);

        int countEntity = txnEntityRepository.findAll().size();
        int countWitness = txnWitnessRepository.findAll().size();
        assertThat(countEntity).isEqualTo(3);
        assertThat(countWitness).isEqualTo(9);
    }
}
