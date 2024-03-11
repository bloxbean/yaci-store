package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceHistoryCleanupHelper {
    private final AccountStoreProperties accountStoreProperties;

    private final DSLContext dsl;
    private final AccountConfigService accountConfigService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

       deleteOldAddressBalances(slot, 50);
       deleteOldStakeAddressBalances(slot, 50);
    }

    private void deleteOldAddressBalances(long slot, int batchSize) {
        Instant start = Instant.now();
        // Fetch all addresses that have changed
        List<String> allAddresses = jdbcTemplate.queryForList(
                "SELECT address FROM address_balance_change_tracker", String.class);

        long count = 0;
        // Process in batches
        for (int i = 0; i < allAddresses.size(); i += batchSize) {
            List<String> batch = allAddresses.subList(i, Math.min(i + batchSize, allAddresses.size()));
            count += deleteAddressBalancesForBatch(batch, slot);
            removeProcessedAddresses(batch);
            log.info("Deleted address balances for batch : " + i);
        }

        log.info("Delete account balance history. # of deleted records : {}", count);
        log.info("Time taken to delete history data before slot: " + slot + " is " + Duration.between(start, Instant.now()).toMillis() + " ms");
    }

    private long deleteAddressBalancesForBatch(List<String> addressBatch, long slot) {
        // Build the SQL IN clause dynamically
        String inSql = String.join(",", java.util.Collections.nCopies(addressBatch.size(), "?"));
        String deleteSql = String.format("""
                    WITH LatestBalances AS (
                          SELECT ab.address, ab.unit, MAX(ab.slot) AS max_slot
                          FROM address_balance ab
                          WHERE ab.address IN (%s)
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
                """, inSql);

        log.info(deleteSql);

        // Prepare parameters (addresses + n)
        Object[] params = new Object[addressBatch.size() + 1];
        for (int i = 0; i < addressBatch.size(); i++) {
            params[i] = addressBatch.get(i);
        }
        params[addressBatch.size()] = slot;

        return jdbcTemplate.update(deleteSql, params);
    }

    private void removeProcessedAddresses(List<String> addresses) {
        // Construct SQL IN clause for batch of addresses
        String inSql = String.join(",", addresses.stream().map(addr -> "'" + addr + "'").toArray(String[]::new));

        String deleteChangedAddressesSql = "DELETE FROM address_balance_change_tracker WHERE address IN (" + inSql + ")";

        jdbcTemplate.update(deleteChangedAddressesSql);
    }

    private void deleteOldStakeAddressBalances(long slot, int batchSize) {
        Instant start = Instant.now();
        // Fetch all addresses that have changed
        List<String> allAddresses = jdbcTemplate.queryForList(
                "SELECT address FROM stake_address_balance_change_tracker", String.class);

        long count = 0;
        // Process in batches
        for (int i = 0; i < allAddresses.size(); i += batchSize) {
            List<String> batch = allAddresses.subList(i, Math.min(i + batchSize, allAddresses.size()));
            count += deleteStakeAddressBalancesForBatch(batch, slot);
            removeProcessedStakeAddresses(batch);
        }

        log.info("Delete stake address balance history. # of deleted records : {}", count);
        log.info("Time taken to delete stake address history data before slot: " + slot + " is " + Duration.between(start, Instant.now()).toMillis() + " ms");
    }

    private long deleteStakeAddressBalancesForBatch(List<String> addressBatch, long slot) {
        // Build the SQL IN clause dynamically
        String inSql = String.join(",", java.util.Collections.nCopies(addressBatch.size(), "?"));
        String deleteSql = String.format("""
                    WITH LatestBalances AS (
                          SELECT ab.address, MAX(ab.slot) AS max_slot
                          FROM stake_address_balance ab
                          WHERE ab.address IN (%s)
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
                """, inSql);

        log.info(deleteSql);

        // Prepare parameters (addresses + n)
        Object[] params = new Object[addressBatch.size() + 1];
        for (int i = 0; i < addressBatch.size(); i++) {
            params[i] = addressBatch.get(i);
        }
        params[addressBatch.size()] = slot;

        return jdbcTemplate.update(deleteSql, params);
    }

    private void removeProcessedStakeAddresses(List<String> addresses) {
        // Construct SQL IN clause for batch of addresses
        String inSql = String.join(",", addresses.stream().map(addr -> "'" + addr + "'").toArray(String[]::new));

        String deleteChangedAddressesSql = "DELETE FROM stake_address_balance_change_tracker WHERE address IN (" + inSql + ")";

        jdbcTemplate.update(deleteChangedAddressesSql);
    }

/**
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
 **/
}
