package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceHistoryCleanupHelper {
    private final AccountStoreProperties accountStoreProperties;

    private final DSLContext dsl;
    private final AccountConfigService accountConfigService;

    //TODO -- Tests
    @Scheduled(fixedRateString = "#{accountStoreProperties.balanceHistoryCleanupInterval * 1000}", initialDelay = 30000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteHistoryDataBeforeSlot() {
        if (!accountStoreProperties.isHistoryCleanupEnabled())
            return;

        var accountConfigOpt = accountConfigService.getConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK);
        Long lastProcessedSlot = accountConfigOpt.map(accountConfigEntity -> accountConfigEntity.getSlot())
                .orElse(null);

        if(lastProcessedSlot == null)
            return;

        long slot = lastProcessedSlot - accountStoreProperties.getBalanceCleanupSlotCount();
        if (slot < 0)
            return;

        log.info("Deleting balance history data before : "
                + (accountStoreProperties.getBalanceCleanupSlotCount() / 86400.0) + " days");

        deleteAddressBalanceHistory(slot);
        deleteStakeAddressBalanceHistory(slot);
    }

    private void deleteAddressBalanceHistory(long slot) {
        Instant start = Instant.now();

        var query = dsl.query("""
                  WITH LatestBalances AS (
                          SELECT ab.address, ab.unit, MAX(ab.slot) AS max_slot
                          FROM address_balance ab
                          INNER JOIN address_balance_change_tracker ca ON ab.address = ca.address
                          GROUP BY ab.address, ab.unit
                      ),
                      DeletableRecords AS (
                          SELECT ab.address, ab.unit, ab.slot
                          FROM address_balance ab
                          JOIN LatestBalances lb ON ab.address = lb.address AND ab.unit = lb.unit
                          WHERE ab.slot < lb.max_slot AND ab.slot < ? 
                      )
                      DELETE FROM address_balance
                          WHERE (address, unit, slot) IN (
                              SELECT address, unit, slot FROM DeletableRecords
                          );
                      
                """, slot);

        if (log.isDebugEnabled())
            log.debug(query.getSQL());

        var count = query.execute();

        var trucateQuery = dsl.query("""
                TRUNCATE TABLE address_balance_change_tracker;
                """);
        trucateQuery.execute();

        log.info("Delete account balance history. # of deleted records : {}", count);
        log.info("Time taken to delete history data before slot: " + slot + " is " + Duration.between(start, Instant.now()).toMillis() + " ms");
    }

    private void deleteStakeAddressBalanceHistory(long slot) {
        Instant start = Instant.now();
        log.info("Deleting history data before slot: " + slot);

        var query = dsl.query("""
                  WITH LatestBalances AS (
                          SELECT ab.address, MAX(ab.slot) AS max_slot
                          FROM stake_address_balance ab
                          INNER JOIN stake_address_balance_change_tracker ca ON ab.address = ca.address
                          GROUP BY ab.address
                      ),
                      DeletableRecords AS (
                          SELECT ab.address, ab.slot
                          FROM stake_address_balance ab
                          JOIN LatestBalances lb ON ab.address = lb.address
                          WHERE ab.slot < lb.max_slot AND ab.slot < ? 
                      )
                      DELETE FROM stake_address_balance
                          WHERE (address, slot) IN (
                              SELECT address, slot FROM DeletableRecords
                          );
                      
                """, slot);

        if (log.isDebugEnabled())
            log.debug(query.getSQL());

        var count = query.execute();

        var trucateQuery = dsl.query("""
                TRUNCATE TABLE stake_address_balance_change_tracker;
                """);
        trucateQuery.execute();

        log.info("Delete stake address balance history. # of deleted records : {}", count);
        log.info("Time taken to delete stake address balance history data before slot: " + slot + " is " + Duration.between(start, Instant.now()).toMillis() + " ms");
    }
}
