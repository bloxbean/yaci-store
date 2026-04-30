package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.MetadataReferenceNftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "store.assets.ext.cip68.enabled", havingValue = "true", matchIfMissing = true)
public class Cip68RollbackProcessor {

    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = metadataReferenceNftRepository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("CIP-68 rollback to slot {}: deleted {} reference NFT records", rollbackSlot, count);
    }

}
