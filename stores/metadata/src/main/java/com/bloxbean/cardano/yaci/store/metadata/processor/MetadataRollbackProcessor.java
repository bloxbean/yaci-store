package com.bloxbean.cardano.yaci.store.metadata.processor;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bloxbean.cardano.yaci.store.metadata.MetadataStoreConfiguration.STORE_METADATA_ENABLED;

@Component
@RequiredArgsConstructor
@Transactional
@EnableIf(STORE_METADATA_ENABLED)
@Slf4j
public class MetadataRollbackProcessor {

    private final TxMetadataStorage txMetadataStorage;

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        //Rows have random ids, so a block processed again must not find its rows still there
        int count = txMetadataStorage.deleteBySlotGreaterThan(rollbackEvent.generatedKeyRollbackSlot());

        log.info("Rollback -- {} transaction_metadata records", count);
    }
}


