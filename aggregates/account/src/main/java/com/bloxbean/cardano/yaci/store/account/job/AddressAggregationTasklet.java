package com.bloxbean.cardano.yaci.store.account.job;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicLong;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;

@RequiredArgsConstructor
@Slf4j
public class AddressAggregationTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;
    private final AccountStoreProperties accountStoreProperties;
    private final static AtomicLong count = new AtomicLong(0);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
        long startOffset = executionContext.getLong(START_OFFSET, 0L); // Default to 0 if not set
        long finalEndOffset = executionContext.getLong(END_OFFSET);

        var snapshotBlock = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getLong(SNAPSHOT_BLOCK);

        var snapshotSlot = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getLong(SNAPSHOT_SLOT);

        long limit = accountStoreProperties.getBalanceCalcJobBatchSize();

        long to = startOffset + limit;

        if (to > finalEndOffset) {
            to = finalEndOffset;
            limit = finalEndOffset - startOffset;
        }

        log.info("Processing addresses from {}, limit: {}, to: {}, FinalEndOffSet {}, stepId: {}", startOffset, limit, to, finalEndOffset, chunkContext.getStepContext().getId());

        calculateAddressBalance(startOffset, limit, snapshotSlot);

        log.info("Snapshot Block: {}, Slot: {} . Total addresses processed: {}", snapshotBlock, snapshotSlot, count.addAndGet(limit));

        // Update ExecutionContext with the new startOffset for the next chunk
        executionContext.putLong(START_OFFSET, Long.valueOf(to));

        boolean shouldContinue = to < finalEndOffset;

        return shouldContinue ? RepeatStatus.CONTINUABLE : RepeatStatus.FINISHED;
    }

    private void calculateAddressBalance(long startOffset, long limit, Long snapshotSlot) {
        String sql = """
                    with incremental as (select address,
                                                unit,
                                                sum(quantity)   as quantity,
                                                max(slot)       as slot,
                                                max(block)      as block,
                                                max(block_time) as block_time,
                                                max(epoch)      as epoch
                                                                    from address_tx_amount ata
                                                                    where ata.address in (select address from address offset ? limit ?)
                                                                    and ata.slot <= ?
                                                                    group by address, unit)
                                               insert
                                               into address_balance (address, unit, quantity, slot, block, block_time, epoch, update_datetime)
                                               select address, unit, quantity, slot, block, block_time, epoch, now()
                                               from incremental
                                               ON CONFLICT (address, unit, slot) DO UPDATE SET
                                                       quantity = EXCLUDED.quantity,
                                                       slot = EXCLUDED.slot,
                                                       block = EXCLUDED.block,
                                                       block_time = EXCLUDED.block_time,
                                                       epoch = EXCLUDED.epoch;                                    
                """;

        jdbcTemplate.update(sql, startOffset, limit, snapshotSlot);
    }

}


