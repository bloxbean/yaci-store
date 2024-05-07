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

        calculateAddressBalance(snapshotSlot, "address_p" + startPartition);

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
                    select address_tx_amount.address         as address,
                           address_tx_amount.unit            as unit,
                           cast(coalesce(
                                   sum(address_tx_amount.quantity),
                                   0
                                ) as decimal(38))            as quantity,
                           max(address_tx_amount.slot)       as slot,
                           max(address_tx_amount.block)      as block,
                           max(address_tx_amount.block_time) as block_time,
                           max(address_tx_amount.epoch)      as epoch,
                           current_timestamp
                    from address_tx_amount
                    where (
                              address_tx_amount.slot <= ?
                                  and address_tx_amount.address in (select address from %s)
                              )
                    group by address_tx_amount.address, address_tx_amount.unit
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


