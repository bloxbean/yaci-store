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
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = txMetadataStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} transaction_metadata records", count);
    }
}


