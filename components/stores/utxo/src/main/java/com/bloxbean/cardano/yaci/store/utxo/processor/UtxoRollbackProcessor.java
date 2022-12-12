package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.repository.InvalidTransactionRepository;
import com.bloxbean.cardano.yaci.store.utxo.repository.UtxoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UtxoRollbackProcessor {
    private final UtxoRepository utxoRepository;
    private final InvalidTransactionRepository invalidTransactionRepository;

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        long rollBackToSlot = rollbackEvent.getRollbackTo().getSlot();
        String rollBackToBlockHash = rollbackEvent.getRollbackTo().getHash();

        utxoRepository.findBySlot(rollBackToSlot)
                        .stream()
                                .forEach(addressUtxo -> {
                                    if (!addressUtxo.getBlockHash().equals(rollBackToBlockHash)) {
                                        log.error("Slot: " + rollBackToSlot + ", blockHash = " + rollBackToBlockHash);
                                        throw new RuntimeException("Rollback slot and blockhash are not matching as expected. Expected Slot: "
                                                + rollBackToSlot + ", blockHash: " + rollBackToBlockHash);
                                    }

                                });

        int deleted = utxoRepository.deleteBySlotGreaterThan(rollBackToSlot);
        int invalidTxnDeleted = invalidTransactionRepository.deleteBySlotGreaterThan(rollBackToSlot);

        log.info("Rollback -- {} address_utxos records", deleted);
        log.info("Rollback -- {} invalid_transactions records", invalidTxnDeleted);

    }
}
