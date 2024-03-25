package com.bloxbean.cardano.yaci.store.account.scheduler;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

//@Component
@RequiredArgsConstructor
@Slf4j
public class QueryBasedAccountBalanceProcessor {
    private final AccountConfigService accountConfigService;
    private final StoreProperties storeProperties;
    private final AccountStoreProperties accountStoreProperties;
    private final JdbcTemplate jdbcTemplate;

    private static final int SNAPSHOT_BATCH_SIZE = 1000;

    private AtomicInteger counter = new AtomicInteger(0);

    @EventListener//(phase = AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAccountBalance(CommitEvent commitEvent) {
        if (commitEvent.getMetadata().isSyncMode())
            return; //don't process in sync mode

        int diff = counter.getAndAdd(storeProperties.getBlocksBatchSize());
        if (diff < SNAPSHOT_BATCH_SIZE) {
            log.info("Counter : " + diff + " is less than batch size : " + SNAPSHOT_BATCH_SIZE);
            return;
        }

        //Take snapshot
        long block = commitEvent.getMetadata().getBlock();
        long newSnapshotBlock = block - storeProperties.getBlocksBatchSize();

        if (newSnapshotBlock < 0)
            return;

        var lastSnapshotBlock = accountConfigService.getConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK)
                .map(accountConfigEntity -> accountConfigEntity.getBlock())
                .orElse(-2L); //Because Block No start from -1 (Genesis Block)

        if (lastSnapshotBlock >= newSnapshotBlock) {
            log.info("Account balance snapshot for block : {} is already taken", newSnapshotBlock);
            return;
        }

        log.info("Taking account balance snapshot for block : " + newSnapshotBlock);

        String dropTempTableQuery = "drop table if exists temp_relevant_transactions";
        String tempTableQuery = String.format("""
                CREATE TEMP TABLE temp_relevant_transactions AS
                SELECT
                    address,
                    unit,
                    max(slot) as slot,
                    max(block) as block,
                    max(block_time) as block_time,
                    SUM(quantity) AS sum_quantity
                FROM
                    address_tx_amount
                WHERE
                    block > %d AND block <= %d
                GROUP BY
                    address, unit
                """, lastSnapshotBlock, newSnapshotBlock);

        String indexSql = """
                CREATE INDEX ON temp_relevant_transactions(address, unit);
                """;
        String sql = String.format("""
                WITH MaxSlotSnapshot AS (
                    SELECT
                        ab.address,
                        ab.unit,
                        MAX(ab.slot) AS max_slot
                    FROM
                        address_balance ab
                        INNER JOIN temp_relevant_transactions rt ON ab.address = rt.address AND ab.unit = rt.unit
                    WHERE
                        ab.block <= %d
                    GROUP BY
                        ab.address, ab.unit
                )
                INSERT INTO address_balance (address, unit, quantity, slot, block, block_time)
                SELECT
                    rt.address,
                    rt.unit,
                    COALESCE(ab.quantity, 0) + rt.sum_quantity AS quantity,
                    rt.slot,
                    rt.block,
                    rt.block_time
                FROM
                    temp_relevant_transactions rt
                    LEFT JOIN (
                        SELECT
                            ab.address,
                            ab.unit,
                            ab.quantity,
                            ab.slot
                        FROM
                            address_balance ab
                            INNER JOIN MaxSlotSnapshot mss ON ab.address = mss.address AND ab.unit = mss.unit AND ab.slot = mss.max_slot
                    ) ab ON rt.address = ab.address AND rt.unit = ab.unit;
                """, lastSnapshotBlock);
//
//        String sql = String.format("""
//                WITH RelevantTransactions AS (
//                    SELECT
//                        address,
//                        unit,
//                        MIN(slot) AS min_slot,
//                        MAX(slot) AS max_slot,
//                        MAX(block) AS max_block,
//                        MAX(block_time) AS max_block_time,
//                        SUM(quantity) AS sum_quantity
//                    FROM
//                        address_tx_amount
//                    WHERE
//                            block > %d AND block <= %d
//                    GROUP BY
//                        address, unit
//                ), LastSnapshotQuantity AS (
//                    SELECT
//                        ab.address,
//                        ab.unit,
//                        ab.quantity,
//                        ab.slot
//                    FROM
//                        address_balance ab
//                            INNER JOIN (
//                            SELECT
//                                address,
//                                unit,
//                                MAX(slot) AS max_slot
//                            FROM
//                                address_balance
//                            WHERE
//                                    block <= %d
//                              AND (address, unit) IN (SELECT address, unit FROM RelevantTransactions)
//                            GROUP BY
//                                address, unit
//                        ) AS MaxSlot ON ab.address = MaxSlot.address AND ab.unit = MaxSlot.unit AND ab.slot = MaxSlot.max_slot
//                )
//                INSERT INTO address_balance (address, unit, quantity, slot, block, block_time)
//                SELECT
//                    rt.address,
//                    rt.unit,
//                    COALESCE(lsq.quantity, 0) + rt.sum_quantity AS quantity,
//                    rt.max_slot,
//                    rt.max_block,
//                    rt.max_block_time
//                FROM
//                    RelevantTransactions rt
//                        LEFT JOIN
//                    LastSnapshotQuantity lsq ON rt.address = lsq.address AND rt.unit = lsq.unit
//                """, lastSnapshotBlock, newSnapshotBlock, lastSnapshotBlock);

//        SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("lastSnapshotBlock", lastSnapshotBlock)
//                        .addValue("newSnapshotBlock", newSnapshotBlock);

        long t1 = System.currentTimeMillis();
        jdbcTemplate.execute(dropTempTableQuery);
        jdbcTemplate.execute(tempTableQuery);
        long t3 = System.currentTimeMillis();
        log.info("Temp table creation took " + (t3 - t1) + " ms");
        jdbcTemplate.execute(indexSql);
        long t4 = System.currentTimeMillis();
        log.info("Index creation took " + (t4 - t3) + " ms");
        int records = jdbcTemplate.update(sql);
        long t2 = System.currentTimeMillis();
        log.info("Account balance snapshot for block : " + newSnapshotBlock + " is taken. Records : " + records + " in " + (t2 - t1) + " ms");

        accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK, null, newSnapshotBlock, null, null); //TODO -- slot

        counter.set(0); //reset
    }
}
