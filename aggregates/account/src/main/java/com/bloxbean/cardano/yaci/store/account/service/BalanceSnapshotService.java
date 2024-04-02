package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.core.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceSnapshotService {
    private final CursorService cursorService;
    private final StartService startService;

    private final JobLauncher jobLauncher;
    private final Job accountBalanceJob;

    @SneakyThrows
    public void scheduleBalanceSnapshot(EventMetadata eventMetadata) {
        startService.stop();

        Thread.startVirtualThread(() -> {
            takeBalanceSnapshot();
        });
    }

    @SneakyThrows
    public void takeBalanceSnapshot() {
        log.info("Taking balance snapshot at current cursor...");
        var cursor = cursorService.getCursor().orElse(null);

        if (cursor == null) {
            log.info("Cursor is null. Skipping balance snapshot");
            return;
        }

        //Take balance snapshot
        log.info("Trying to take balance snapshot at block : {}, slot: {}", cursor.getBlock(), cursor.getSlot());
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addLong(SNAPSHOT_BLOCK, cursor.getBlock())
                .addLong(SNAPSHOT_SLOT, cursor.getSlot())
                .addString(SNAPSHOT_BLOCKHASH, cursor.getBlockHash())
                .addString(UPDATE_ACCOUNT_CONFIG, "true")
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(accountBalanceJob, jobParameters);
        log.info("Job Execution Status: " + jobExecution.getStatus());
    }

}
