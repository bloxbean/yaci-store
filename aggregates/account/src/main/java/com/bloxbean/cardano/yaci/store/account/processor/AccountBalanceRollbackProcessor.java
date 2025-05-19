package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.BalanceRollbackEvent;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.bloxbean.cardano.yaci.store.account.AccountStoreConfiguration.STORE_ACCOUNT_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(value = STORE_ACCOUNT_ENABLED, defaultValue = false)
@Slf4j
public class AccountBalanceRollbackProcessor {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountStoreProperties accountStoreProperties;
    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {

        if (accountStoreProperties.isContentAwareRollback()) {
            handleContentAwareBalanceRollback(rollbackEvent);
        }

        //Check rollbackTo slot
        int addressBalanceDeleted = accountBalanceStorage.deleteAddressBalanceBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        int stakeBalanceDeleted = accountBalanceStorage.deleteStakeAddressBalanceBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} address_balance records", addressBalanceDeleted);
        log.info("Rollback -- {} stake_balance records", stakeBalanceDeleted);
    }

    private void handleContentAwareBalanceRollback(RollbackEvent rollbackEvent) {
        var rolledBackAddressBalances = accountBalanceStorage.getAddressBalanceBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        var rolledBackStakeAddressBalances = accountBalanceStorage.getStakeAddressBalanceBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        Map<String, Set<String>> addressUnitMap = new HashMap<>();

        for (var addressBalance : rolledBackAddressBalances) {
            String address = addressBalance.getAddress();
            String unit = addressBalance.getUnit();

            if (addressUnitMap.containsKey(address)) {
                addressUnitMap.get(address).add(unit);
            } else {
                Set<String> units = new HashSet<>();
                units.add(unit);
                addressUnitMap.put(address, units);
            }
        }

        //Address unit maps to AddressUnits record
        List<BalanceRollbackEvent.AddressUnits> addressUnits = new ArrayList<>();
        for (var entry : addressUnitMap.entrySet()) {
            String address = entry.getKey();
            Set<String> units = entry.getValue();

            addressUnits.add(new BalanceRollbackEvent.AddressUnits(address, units));
        }

        var stakeAddresses = rolledBackStakeAddressBalances
                .stream()
                .map(stakeAddressBalance -> stakeAddressBalance.getAddress())
                .distinct()
                .toList();

        var balanceRollbackEvent = new BalanceRollbackEvent(rollbackEvent, addressUnits, stakeAddresses);
        applicationEventPublisher.publishEvent(balanceRollbackEvent);
    }

    @EventListener
    @Transactional
    public void handleCurrentAddressBalanceForRollback(BalanceRollbackEvent balanceRollbackEvent) {
        if (!accountStoreProperties.isCurrentBalanceEnabled())
            return;

        for (BalanceRollbackEvent.AddressUnits addressUnits: balanceRollbackEvent.getAddressUnits()) {
            accountBalanceStorage.refreshCurrentAddressBalance(addressUnits.getAddress(), addressUnits.getUnits(), balanceRollbackEvent.getRollbackEvent().getRollbackTo().getSlot());
        }

        accountBalanceStorage.refreshCurrentStakeAddressBalance(balanceRollbackEvent.getStakeAddresses(), balanceRollbackEvent.getRollbackEvent().getRollbackTo().getSlot());

        log.info("Rollback -- Address balance adjusted for {} addresses", balanceRollbackEvent.getAddressUnits().size());
        log.info("Rollback -- Stake address balance adjusted for {} addresses", balanceRollbackEvent.getStakeAddresses().size());

        if (log.isDebugEnabled()) {
            log.debug("Rollback -- Address balance adjusted for {}", balanceRollbackEvent.getAddressUnits());
            log.debug("Rollback -- Stake address balance adjusted for {}", balanceRollbackEvent.getStakeAddresses());
        }
    }
}
