package com.bloxbean.cardano.yaci.store.account.job;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicLong;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;

@RequiredArgsConstructor
@Slf4j
public class BalanceByHashedBaseAggregationTasklet implements Tasklet {

    private final AccountStoreProperties accountStoreProperties;
    private final DSLContext dsl;
    private final PlatformTransactionManager transactionManager;

    private final static AtomicLong count = new AtomicLong(0);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
        long startPartition = executionContext.getLong(START_PARTITION, 0L); // Default to 0 if not set
        long endPartition = executionContext.getLong(END_PARTITION);

        var snapshotSlot = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getLong(SNAPSHOT_SLOT);

        long to = startPartition + 1;

        long startTime = System.currentTimeMillis();

        log.info("Processing partition {}, endPartition {}, stepId: {}", startPartition, endPartition, chunkContext.getStepContext().getId());

        calculateAddressBalance(snapshotSlot, "address_tx_amount_p" + startPartition);

        log.info("Processed partition {}, endPartition {}, stepId: {}, take [{} ms]", startPartition, endPartition, chunkContext.getStepContext().getId(), System.currentTimeMillis() - startTime);

        // Update ExecutionContext with the new startOffset for the next chunk
        executionContext.putLong(START_PARTITION, to);

        boolean shouldContinue = to <= endPartition;

        return shouldContinue ? RepeatStatus.CONTINUABLE : RepeatStatus.FINISHED;
    }

    private void calculateAddressBalance(Long snapshotSlot, String partitionName) {
        String sql = String.format("""
                insert into address_balance_snapshot (address,
                                                      unit,
                                                      quantity,
                                                      slot,
                                                      block,
                                                      block_time,
                                                      epoch,
                                                      update_datetime)
                select ata.address            as address,
                       ata.unit               as unit,
                       cast(coalesce(
                               sum(ata.quantity),
                               0
                            ) as decimal(38)) as quantity,
                       max(ata.slot)          as slot,
                       max(ata.block)         as block,
                       max(ata.block_time)    as block_time,
                       max(ata.epoch)         as epoch,
                       current_timestamp
                from %s ata
                where (ata.slot <= ?)
                group by ata.address, ata.unit
                on conflict (address, unit, slot)
                    do update
                    set quantity   = excluded.quantity,
                        slot       = excluded.slot,
                        block      = excluded.block,
                        block_time = excluded.block_time,
                        epoch      = excluded.epoch;
                """, partitionName);

        var insertQuery = dsl.query(sql, snapshotSlot);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.execute(status -> {
            insertQuery.queryTimeout(300).execute();
            return true;
        });
    }

}


