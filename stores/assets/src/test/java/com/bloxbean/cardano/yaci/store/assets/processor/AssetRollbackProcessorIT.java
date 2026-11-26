package com.bloxbean.cardano.yaci.store.assets.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.repository.TxAssetRepository;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
class AssetRollbackProcessorIT {

    @Autowired
    private AssetRollbackProcessor assetRollbackProcessor;

    @Autowired
    private TxAssetRepository txAssetRepository;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/assets_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteAssets() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(10702061, "5068285d5eccc63bbfe0caa446fde9c28d77e45b6f34d37c29bbae8d47c30b9b"))
                .currentPoint(new Point(10707861, "5ca2e98fe743c4dc92b323a6cd244825e663aa1e35fd3123487c8c0a170196e2"))
                .build();

        assetRollbackProcessor.handleRollbackEvent(rollbackEvent);

        int count = txAssetRepository.findAll().size();
        assertThat(count).isEqualTo(4);
    }
}
