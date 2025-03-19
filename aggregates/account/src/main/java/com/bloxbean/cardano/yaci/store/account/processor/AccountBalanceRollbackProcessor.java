package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bloxbean.cardano.yaci.store.account.AccountStoreConfiguration.STORE_ACCOUNT_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(value = STORE_ACCOUNT_ENABLED, defaultValue = false)
@Slf4j
public class AccountBalanceRollbackProcessor {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountConfigService accountConfigService;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        //Check rollbackTo slot
        int addressBalanceDeleted = accountBalanceStorage.deleteAddressBalanceBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        int stakeBalanceDeleted = accountBalanceStorage.deleteStakeAddressBalanceBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} address_balance records", addressBalanceDeleted);
        log.info("Rollback -- {} stake_balance records", stakeBalanceDeleted);
    }
}
