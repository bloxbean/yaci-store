package com.bloxbean.cardano.yaci.store.account.job;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.concurrent.atomic.AtomicLong;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.ADDRESS_TX_AMOUNT;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.STAKE_ADDRESS_BALANCE;
import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
@Slf4j
public class StakeAddressAggregationTasklet implements Tasklet {

    private final AccountStoreProperties accountStoreProperties;
    private final DSLContext dsl;
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

        long batchSize = accountStoreProperties.getBalanceCalcJobBatchSize();

        long to = startOffset + batchSize;

        if (to > finalEndOffset) {
            to = finalEndOffset;
            batchSize = finalEndOffset - startOffset;
        }

        log.info("Processing stake addresses from {}, batchSize: {}, to: {}, FinalEndOffSet {}, stepId: {}", startOffset, batchSize, to, finalEndOffset, chunkContext.getStepContext().getId());

        calculateStakeAddressBalance(startOffset, to, snapshotSlot);

        log.info("Total stake addresses processed: {}", count.addAndGet(batchSize));

        // Update ExecutionContext with the new startOffset for the next chunk
        executionContext.putLong(START_OFFSET, Long.valueOf(to));

        boolean shouldContinue = to < finalEndOffset;

        return shouldContinue ? RepeatStatus.CONTINUABLE : RepeatStatus.FINISHED;
    }

    private void calculateStakeAddressBalance(long startOffset, long to, Long snapshotSlot) {
        dsl.insertInto(STAKE_ADDRESS_BALANCE,
                        STAKE_ADDRESS_BALANCE.ADDRESS,
                        STAKE_ADDRESS_BALANCE.QUANTITY,
                        STAKE_ADDRESS_BALANCE.SLOT,
                        STAKE_ADDRESS_BALANCE.BLOCK,
                        STAKE_ADDRESS_BALANCE.BLOCK_TIME,
                        STAKE_ADDRESS_BALANCE.EPOCH,
                        STAKE_ADDRESS_BALANCE.UPDATE_DATETIME
                ).select(select(field(ADDRESS_TX_AMOUNT.STAKE_ADDRESS).as("address"),
                        coalesce(sum(field(ADDRESS_TX_AMOUNT.QUANTITY)), 0).cast(SQLDataType.DECIMAL_INTEGER(38)).as("quantity"),
                        max(field(ADDRESS_TX_AMOUNT.SLOT)).as("slot"),
                        max(field(ADDRESS_TX_AMOUNT.BLOCK)).as("block"),
                        max(field(ADDRESS_TX_AMOUNT.BLOCK_TIME)).as("block_time"),
                        max(field(ADDRESS_TX_AMOUNT.EPOCH)).as("epoch"),
                        currentLocalDateTime()
                )
                        .from(ADDRESS_TX_AMOUNT)
                        .where(ADDRESS_TX_AMOUNT.STAKE_ADDRESS.isNotNull()
                                .and(ADDRESS_TX_AMOUNT.SLOT.le(snapshotSlot))
                                .and(ADDRESS_TX_AMOUNT.STAKE_ADDRESS
                                        .in(select(field("address", String.class))
                                                .from(table(STAKE_ADDRESS_TEMP_TABLE))
                                                .where(field("id", Long.class).between(startOffset, to))
                                        )
                                ).and(ADDRESS_TX_AMOUNT.UNIT.eq(LOVELACE))
                        )
                        .groupBy(ADDRESS_TX_AMOUNT.STAKE_ADDRESS))
                .onConflict(
                        STAKE_ADDRESS_BALANCE.ADDRESS,
                        STAKE_ADDRESS_BALANCE.SLOT
                )
                .doUpdate()
                .set(STAKE_ADDRESS_BALANCE.QUANTITY, excluded(STAKE_ADDRESS_BALANCE.QUANTITY))
                .set(STAKE_ADDRESS_BALANCE.SLOT, excluded(STAKE_ADDRESS_BALANCE.SLOT))
                .set(STAKE_ADDRESS_BALANCE.BLOCK, excluded(STAKE_ADDRESS_BALANCE.BLOCK))
                .set(STAKE_ADDRESS_BALANCE.BLOCK_TIME, excluded(STAKE_ADDRESS_BALANCE.BLOCK_TIME))
                .set(STAKE_ADDRESS_BALANCE.EPOCH, excluded(STAKE_ADDRESS_BALANCE.EPOCH))
                .execute();
    }

}


