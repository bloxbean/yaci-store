package com.bloxbean.cardano.yaci.store.account.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
@Slf4j
public class AddressAggregationTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    private long batchSize = 1000;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
        long startOffset = executionContext.getLong("startOffset", 0L); // Default to 0 if not set
        long finalEndOffset = executionContext.getLong("endOffset");

        long limit = batchSize;

        long endOffset = startOffset + batchSize;

        if (endOffset > finalEndOffset) {
            endOffset = finalEndOffset;
            limit = finalEndOffset - startOffset;
        }

        log.info("Processing addresses from {}, limit: {}, endOffSet: {}, FinalEndOffSet {}, stepId: {}", startOffset, limit, endOffset, finalEndOffset, chunkContext.getStepContext().getId());

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
                                                                    group by address, unit)
                                               insert
                                               into address_balance
                                               select *
                                               from incremental
                                               ON CONFLICT (address, unit, slot) DO UPDATE SET
                                                       quantity = EXCLUDED.quantity,
                                                       slot = EXCLUDED.slot,
                                                       block = EXCLUDED.block,
                                                       block_time = EXCLUDED.block_time,
                                                       epoch = EXCLUDED.epoch;                                    
                """;

        jdbcTemplate.update(sql, startOffset, limit);

        // Update ExecutionContext with the new startOffset for the next chunk
        executionContext.putLong("startOffset", Long.valueOf(endOffset));

        boolean shouldContinue = endOffset < finalEndOffset;

        return shouldContinue ? RepeatStatus.CONTINUABLE : RepeatStatus.FINISHED;
    }
}


