package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.core.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceSnapshotService {
    private final CursorService cursorService;
    private final StartService startService;
    private final DSLContext dsl;

    private final JobLauncher jobLauncher;
    private final Job accountBalanceJob;
    private final JobRepository jobRepository;

    private final AccountConfigService accountConfigService;

    private final AtomicBoolean isTakingBalanceSnapshot = new AtomicBoolean(false);

    @SneakyThrows
    public void scheduleBalanceSnapshot(EventMetadata eventMetadata) {
        startService.stop();
        isTakingBalanceSnapshot.set(true);
    }

    @Scheduled(fixedRate = 10000)
    @SneakyThrows
    public void takeBalanceSnapshot() {
        if(!isTakingBalanceSnapshot.get())
            return;

        log.info("Taking balance snapshot at current cursor...");
        var cursor = cursorService.getCursor().orElse(null);

        if (cursor == null) {
            log.info("Cursor is null. Skipping balance snapshot");
            return;
        }

        //Check the balance calc block/slot in account config
        var accountConfigOpt = accountConfigService.getConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK);
        Long lastProcessedBlock = accountConfigOpt.map(accountConfigEntity -> accountConfigEntity.getBlock())
                .orElse(0L);

        if(lastProcessedBlock >= cursor.getBlock()) {
            log.info("Last processed block is greater than or equal to current block. Restart sync process");
            startService.start();
            return;
        }

        //Take balance snapshot
        log.info("Taking balance snapshot at block : " + cursor.getBlock());
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("block", cursor.getBlock())
                .toJobParameters();

        var jobInstance = jobRepository.getJobInstance(accountBalanceJob.getName(), jobParameters);
        if (jobInstance != null) {
            log.info("Job already exists for block : " + cursor.getBlock());
            return;
        }

        var lastJobExecution = jobRepository.getLastJobExecution(accountBalanceJob.getName(), jobParameters);
        log.info("Last Job Execution : " + lastJobExecution);


        // Launch the job
        JobExecution jobExecution = jobLauncher.run(accountBalanceJob, jobParameters);
        System.out.println("Job Execution Status: " + jobExecution.getStatus());

        //Configure account config

    }
//
//    private void calculateAddressBalance(long addressOffset, long end) {
//        long startTime = System.currentTimeMillis();
//       // long addressOffset = 45_000_000;
//        long batchSize = 6_000;
//       // long end = 46_000_000;
//
//        partition(addressOffset, end, batchSize, (iOffset) -> this.aggregateParallel(iOffset, batchSize));
//
//        log.info("Calculate address balance summary take [{} ms]", System.currentTimeMillis() - startTime);
//        accountConfigService.upateConfig();
//
//    }
//
//    @SneakyThrows
//    private void partition(long from, long to, long batchSize, Consumer<Long> applyFunc) {
//        List<CompletableFuture> futures = new ArrayList<>();
//        long current = from;
//        while (current < to) {
//            long finalCurrent = current;
//            var completableFuture = CompletableFuture.supplyAsync(() -> {
//                applyFunc.accept(finalCurrent);
//                return true;
//            });
//            futures.add(completableFuture);
//            current += batchSize;
//        }
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//        for (var future : futures) {
//            future.get();
//        }
//    }
//
//    private void aggregateParallel(Long iOffset, long iLimit) {
//        long beforeInsert = System.currentTimeMillis();
//        String sql = """
//                with incremental as (select address,
//                                                unit,
//                                                sum(quantity)   as quantity,
//                                                max(slot)       as slot,
//                                                max(block)      as block,
//                                                max(block_time) as block_time,
//                                                max(epoch)      as epoch
//                                         from address_tx_amount ata
//                                         where ata.address in (select address from address offset ? limit ?)
//                                         group by address, unit)
//                    insert
//                    into address_balance
//                    select *
//                    from incremental;
//            """;
//
//        var query = dsl.query(sql, iOffset, iLimit);
//        long count = query.execute();
//        log.info("From offset {} to {}, Insert {} rows, take [{}ms]", iOffset, iOffset + iLimit, count, System.currentTimeMillis() - beforeInsert);
//    }

}
