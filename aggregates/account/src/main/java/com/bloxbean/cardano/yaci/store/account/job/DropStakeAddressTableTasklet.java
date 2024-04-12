package com.bloxbean.cardano.yaci.store.account.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.STAKE_ADDRESS_TEMP_TABLE;

@RequiredArgsConstructor
@Slf4j
public class DropStakeAddressTableTasklet implements Tasklet {
    private final DSLContext dsl;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("<< Cleaning up temp table for stake addresses >>");
        dsl.dropTableIfExists(STAKE_ADDRESS_TEMP_TABLE).execute();

        log.info("<< Cleaned up temp table for stake addresses >>");

        dsl.dropIndex("idx_address_utxo_owner_addr").execute();
        dsl.dropIndex("idx_address_utxo_owner_stake_addr").execute();

        log.info("<< Dropped index on address_utxo >>");
        return RepeatStatus.FINISHED;
    }
}
