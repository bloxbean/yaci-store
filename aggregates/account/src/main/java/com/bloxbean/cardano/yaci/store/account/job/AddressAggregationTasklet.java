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

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
@Slf4j
public class AddressAggregationTasklet implements Tasklet {

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

    private void calculateAddressBalance(long startOffset, long to, Long snapshotSlot) {
        var insertQuery = dsl.insertInto(ADDRESS_BALANCE,
                        ADDRESS_BALANCE.ADDRESS,
                        ADDRESS_BALANCE.UNIT,
                        ADDRESS_BALANCE.QUANTITY,
                        ADDRESS_BALANCE.SLOT,
                        ADDRESS_BALANCE.BLOCK,
                        ADDRESS_BALANCE.BLOCK_TIME,
                        ADDRESS_BALANCE.EPOCH,
                        ADDRESS_BALANCE.UPDATE_DATETIME
                ).select(select(field(ADDRESS_TX_AMOUNT.ADDRESS).as("address"),
                                field(ADDRESS_TX_AMOUNT.UNIT).as("unit"),
                                coalesce(sum(field(ADDRESS_TX_AMOUNT.QUANTITY)), BigDecimal.ZERO).cast(SQLDataType.DECIMAL_INTEGER(38)).as("quantity"),
                                max(field(ADDRESS_TX_AMOUNT.SLOT)).as("slot"),
                                max(field(ADDRESS_TX_AMOUNT.BLOCK)).as("block"),
                                max(field(ADDRESS_TX_AMOUNT.BLOCK_TIME)).as("block_time"),
                                max(field(ADDRESS_TX_AMOUNT.EPOCH)).as("epoch"),
                                currentLocalDateTime()
                        )
                                .from(ADDRESS_TX_AMOUNT)
                                .where(ADDRESS_TX_AMOUNT.SLOT.le(snapshotSlot)
                                        .and(ADDRESS_TX_AMOUNT.ADDRESS
                                                .in(select(ADDRESS.ADDRESS_)
                                                        .from(ADDRESS)
                                                        .where(ADDRESS.ID.between(startOffset, to))
                                                )
                                        )
                                )
                                .groupBy(ADDRESS_TX_AMOUNT.ADDRESS, ADDRESS_TX_AMOUNT.UNIT)
                )
                .onConflict(
                        ADDRESS_BALANCE.ADDRESS,
                        ADDRESS_BALANCE.UNIT,
                        ADDRESS_BALANCE.SLOT
                )
                .doUpdate()
                .set(ADDRESS_BALANCE.QUANTITY, excluded(ADDRESS_BALANCE.QUANTITY))
                .set(ADDRESS_BALANCE.SLOT, excluded(ADDRESS_BALANCE.SLOT))
                .set(ADDRESS_BALANCE.BLOCK, excluded(ADDRESS_BALANCE.BLOCK))
                .set(ADDRESS_BALANCE.BLOCK_TIME, excluded(ADDRESS_BALANCE.BLOCK_TIME))
                .set(ADDRESS_BALANCE.EPOCH, excluded(ADDRESS_BALANCE.EPOCH));
        insertQuery.execute();
    }

}


