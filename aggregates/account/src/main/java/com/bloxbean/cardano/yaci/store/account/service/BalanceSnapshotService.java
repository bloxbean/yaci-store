package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.ADDRESS_BALANCE;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.STAKE_ADDRESS_BALANCE;

@Service
@Slf4j
public class BalanceSnapshotService {
    private final static String UTXO_BATCH_MODE = "utxo";
    private final static String TX_AMOUNT_BATCH_MODE = "tx-amount";

    private final CursorService cursorService;
    private final StartService startService;
    private final EraService eraService;
    private final AccountStoreProperties accountStoreProperties;
    private DSLContext dsl;

    private final JobLauncher jobLauncher;
    private final Job accountBalanceJob;
    private final Job utxoAccountBalanceJob;

    public BalanceSnapshotService(CursorService cursorService,
                                  StartService startService,
                                  EraService eraService,
                                  AccountStoreProperties accountStoreProperties,
                                  DSLContext dsl,
                                  JobLauncher jobLauncher,
                                  @Autowired(required = false) Job accountBalanceJob,
                                  @Autowired(required = false) Job utxoAccountBalanceJob) {
        this.cursorService = cursorService;
        this.startService = startService;
        this.eraService = eraService;
        this.accountStoreProperties = accountStoreProperties;
        this.dsl = dsl;

        this.jobLauncher = jobLauncher;
        this.accountBalanceJob = accountBalanceJob;
        this.utxoAccountBalanceJob = utxoAccountBalanceJob;
    }

    /**
     * Schedule balance snapshot at current cursor block
     * @return
     */
    @SneakyThrows
    public synchronized boolean scheduleBalanceSnapshot() {
        if (accountBalanceJob == null && utxoAccountBalanceJob == null) {
            log.info("Balance snapshot job is not enabled. Skipping balance snapshot.");
            return false;
        }

        if (!startService.isStarted()) {
            log.info("Looks like sync process has not been started or balanced snapshot is in progress. Skipping balance snapshot");
            return false;
        }

        startService.stop();

        Thread.startVirtualThread(() -> {
            takeBalanceSnapshot();
        });

        return true;
    }

    /**
     * Take balance snapshot at a previous block
     * @param block
     * @param slot
     * @param blockHash
     */
    @SneakyThrows
    public synchronized boolean scheduleBalanceSnapshot(long block, long slot, String blockHash) {
        log.info("Taking balance snapshot ...");
        if (accountBalanceJob == null && utxoAccountBalanceJob == null) {
            log.info("Balance snapshot job is not enabled. Skipping balance snapshot.");
            return false;
        }

        var cursor = cursorService.getCursor().orElse(null);

        if (cursor == null) {
            log.info("Cursor is null. Skipping balance snapshot");
            return false;
        }

        if (block >= cursor.getBlock()) {
            log.warn("Block : {} is greater than or equal to current cursor : {}. Skipping balance snapshot", block, cursor.getBlock());
            return false;
        }

        Thread.startVirtualThread(() -> {
            long epoch = 0L;
            long blockTime = 0L;
            if (cursor.getEra() != null && cursor.getSlot() > 0) {
                epoch = eraService.getEpochNo(cursor.getEra(), cursor.getSlot());
                blockTime = eraService.blockTime(cursor.getEra(), cursor.getSlot());
            }

            takeBalanceSnapshot(block, slot, blockHash, epoch, blockTime, false);
        });

        return true;
    }

    /**
     * Take balance snapshot at current cursor block
     * @return
     */
    @SneakyThrows
    private boolean takeBalanceSnapshot() {
        log.info("Taking balance snapshot ...");
        TimeUnit.SECONDS.sleep(5); //sleep for 5 seconds to allow the pending commit

        var cursor = cursorService.getCursor().orElse(null);

        if (cursor == null) {
            log.info("Cursor is null. Skipping balance snapshot");
            return false;
        }

        long epoch = 0L;
        long blockTime = 0L;
        if (cursor.getEra() != null && cursor.getSlot() > 0) {
            epoch = eraService.getEpochNo(cursor.getEra(), cursor.getSlot());
            blockTime = eraService.blockTime(cursor.getEra(), cursor.getSlot());
        }

        takeBalanceSnapshot(cursor.getBlock(), cursor.getSlot(), cursor.getBlockHash(), epoch, blockTime, true);
        return true;
    }

    @SneakyThrows
    private void takeBalanceSnapshot(long block, long slot, String blockHash, long epoch, long blockTime, boolean updateConfig) {
        //Take balance snapshot
        log.info("Trying to take balance snapshot at block : {}, slot: {}", block, slot);
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong(SNAPSHOT_BLOCK, block)
                .addLong(SNAPSHOT_SLOT, slot)
                .addString(SNAPSHOT_BLOCKHASH, blockHash)
                .addLong(SNAPSHOT_EPOCH, epoch)
                .addLong(SNAPSHOT_BLOCKTIME, blockTime)
                .addString(UPDATE_ACCOUNT_CONFIG, updateConfig? "true": "false")
                .toJobParameters();

        if (accountStoreProperties.getBalanceCalcBatchMode().equals(UTXO_BATCH_MODE)) {
            log.info("Running UTXO balance snapshot job >>");

            if (alreadyHashBalanceRecords()) {
                log.warn("Balance records already exist. Balance snapshot job will not run.");
                return;
            }

            JobExecution jobExecution = jobLauncher.run(utxoAccountBalanceJob, jobParameters);
            log.info("Job Execution Status: " + jobExecution.getStatus());
        } else if (accountStoreProperties.getBalanceCalcBatchMode().equals(TX_AMOUNT_BATCH_MODE)) {
            log.info("Running TX_AMOUNT balance snapshot job >>");
            JobExecution jobExecution = jobLauncher.run(accountBalanceJob, jobParameters);
            log.info("Job Execution Status: " + jobExecution.getStatus());
        } else {
            log.error("Invalid balance-calc-batch-mode : {}. Skipping balance snapshot", accountStoreProperties.getBalanceCalcBatchMode());
        }
    }

    private boolean alreadyHashBalanceRecords() {
        boolean addressBalanceExists = dsl.fetchExists(dsl.selectOne()
                .from(ADDRESS_BALANCE)
                .limit(1));

        boolean stakeAddressBalanceExists = dsl.fetchExists(dsl.selectOne()
                .from(STAKE_ADDRESS_BALANCE)
                .limit(1));

        if (addressBalanceExists)
            log.warn("Address balance records already exist. Balance snapshot job will not run. " +
                    "Truncate address_balance table to run the job.");
        if (stakeAddressBalanceExists)
            log.warn("Stake address balance records already exist. Balance snapshot job will not run. " +
                    "Truncate stake_address_balance table to run the job.");

        return addressBalanceExists || stakeAddressBalanceExists;
    }

}
