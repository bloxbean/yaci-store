package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.domain.Deposit;
import com.bloxbean.cardano.yaci.store.adapot.storage.DepositStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositProcessor {

    private final DepositRules depositRules;
    private final DepositStorage depositStorage;

    private List<TransactionEvent> transactionEventsCache = new ArrayList<>();

    @EventListener
    @Transactional
    public void processDeposit(TransactionEvent event) {
        if (event.getMetadata().getEra() == Era.Byron)
            return;

        transactionEventsCache.add(event);
    }

    @EventListener
    @Transactional
    public void handleCommitEvent(CommitEvent commitEvent) {
        if (transactionEventsCache.isEmpty())
            return;

        List<Deposit> deposits = new ArrayList<>();
        for (TransactionEvent event : transactionEventsCache) {

            for (var transaction : event.getTransactions()) {
                //process deposits & refunds
                var blockDeposits = depositRules.findDepositAndRefund(event.getMetadata(), transaction);
                if (blockDeposits != null && !blockDeposits.isEmpty()) {
                    deposits.addAll(blockDeposits);
                }
            }
        }

        if (!deposits.isEmpty()) {
            depositStorage.save(deposits);
        }

        transactionEventsCache.clear();
    }

    @EventListener
    @Transactional
    public void rollback(RollbackEvent rollbackEvent) {
        transactionEventsCache.clear();
        int count = depositStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} deposit records", count);
    }

}
