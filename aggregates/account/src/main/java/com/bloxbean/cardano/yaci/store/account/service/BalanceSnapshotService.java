package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.core.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceSnapshotService {
    private final CursorService cursorService;
    private final StartService startService;

    private final JobLauncher jobLauncher;
    private final Job accountBalanceJob;

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
            takeBalanceSnapshot(block, slot, blockHash, false);
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

        takeBalanceSnapshot(cursor.getBlock(), cursor.getSlot(), cursor.getBlockHash(), true);
        return true;
    }

    @SneakyThrows
    private void takeBalanceSnapshot(long block, long slot, String blockHash, boolean updateConfig) {
        //Take balance snapshot
        log.info("Trying to take balance snapshot at block : {}, slot: {}", block, slot);
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addLong(SNAPSHOT_BLOCK, block)
                .addLong(SNAPSHOT_SLOT, slot)
                .addString(SNAPSHOT_BLOCKHASH, blockHash)
                .addString(UPDATE_ACCOUNT_CONFIG, updateConfig? "true": "false")
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(accountBalanceJob, jobParameters);
        log.info("Job Execution Status: " + jobExecution.getStatus());
    }

}
