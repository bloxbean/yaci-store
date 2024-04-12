package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.core.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceSnapshotService {
    private final static String UTXO_BATCH_MODE = "utxo";
    private final static String TX_AMOUNT_BATCH_MODE = "tx-amount";

    private final CursorService cursorService;
    private final StartService startService;
    private final EraService eraService;

    private final JobLauncher jobLauncher;
    private final Job accountBalanceJob;
    private final Job utxoAccountBalanceJob;

    @Value("${store.account.balance-calc-batch-mode:utxo}")
    private String balanceCalcBatchMode;

    /**
     * Schedule balance snapshot at current cursor block
     * @return
     */
    @SneakyThrows
    public synchronized boolean scheduleBalanceSnapshot() {
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

        if (balanceCalcBatchMode.equals(UTXO_BATCH_MODE)) {
            log.info("Running UTXO balance snapshot job >>");
            JobExecution jobExecution = jobLauncher.run(utxoAccountBalanceJob, jobParameters);
            log.info("Job Execution Status: " + jobExecution.getStatus());
        } else if (balanceCalcBatchMode.equals(TX_AMOUNT_BATCH_MODE)) {
            log.info("Running TX_AMOUNT balance snapshot job >>");
            JobExecution jobExecution = jobLauncher.run(accountBalanceJob, jobParameters);
            log.info("Job Execution Status: " + jobExecution.getStatus());
        } else {
            log.error("Invalid balance-calc-batch-mode : {}. Skipping balance snapshot", balanceCalcBatchMode);
        }
    }

}
