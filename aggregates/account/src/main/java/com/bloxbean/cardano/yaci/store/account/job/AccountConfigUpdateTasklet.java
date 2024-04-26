package com.bloxbean.cardano.yaci.store.account.job;

import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.account.util.ConfigStatus;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;

@RequiredArgsConstructor
@Slf4j
public class AccountConfigUpdateTasklet implements Tasklet {

    private final AccountConfigService accountConfigService;
    private final StartService startService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var snapshotBlock = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getLong(SNAPSHOT_BLOCK);

        var snapshotBlockHash = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getString(SNAPSHOT_BLOCKHASH);

        var snapshotSlot = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getLong(SNAPSHOT_SLOT);

        var updateAccountConfig = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getString(AccountJobConstants.UPDATE_ACCOUNT_CONFIG);

        if (updateAccountConfig == null || !updateAccountConfig.equals("true")) {
            log.info("Update account config is not set to true. Skipping account config update");
            return RepeatStatus.FINISHED;
        }

        log.info(">>> Updating account config with snapshot block: {}, snapshot block hash: {}, snapshot slot: {}", snapshotBlock, snapshotBlockHash, snapshotSlot);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.execute(status -> {
            accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK, ConfigStatus.BALANCE_SNAPSHOT,
                    snapshotBlock, snapshotBlockHash, snapshotSlot);
            return null;
        });

        log.info("<<<< Starting the sync process after updating account config >>>>");

        Thread.startVirtualThread(() -> {
            log.info("Waiting for 10 seconds before starting the sync process ...");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
            }
            startService.start();
        });

        return RepeatStatus.FINISHED;
    }
}


