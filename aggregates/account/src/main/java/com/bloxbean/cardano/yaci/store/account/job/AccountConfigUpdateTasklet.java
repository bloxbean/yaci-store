package com.bloxbean.cardano.yaci.store.account.job;

import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;

@RequiredArgsConstructor
@Slf4j
public class AccountConfigUpdateTasklet implements Tasklet {

    private final AccountConfigService accountConfigService;
    private final StartService startService;

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

        accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK, null, snapshotBlock, snapshotBlockHash, snapshotSlot);

        startService.start();

        return RepeatStatus.FINISHED;
    }
}


