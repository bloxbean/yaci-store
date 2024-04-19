package com.bloxbean.cardano.yaci.store.account.job.utxo;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.*;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.ADDRESS_BALANCE;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
@Slf4j
public class AddressUtxoAggregationTasklet implements Tasklet {

    private final AccountStoreProperties accountStoreProperties;
    private final DSLContext dsl;
    private final PlatformTransactionManager transactionManager;

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

        var snapshotEpoch = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getLong(SNAPSHOT_EPOCH);

        var snapshotBlockTime = chunkContext.getStepContext().getStepExecution()
                .getJobParameters().getLong(SNAPSHOT_BLOCKTIME);

        long batchSize = accountStoreProperties.getBalanceCalcJobBatchSize();

        long to = startOffset + batchSize;

        if (to > finalEndOffset) {
            to = finalEndOffset;
        }

        log.info("<< Processing addresses from {}, to: {}, FinalEndOffSet {}, stepId: {} >>", startOffset, to, finalEndOffset, chunkContext.getStepContext().getId());

        calculateAddressBalance(startOffset, to, snapshotSlot, snapshotBlock, snapshotBlockTime, snapshotEpoch.intValue());

        log.info("<< Snapshot Block: {}, Slot: {} . Total addresses processed: {} >>", snapshotBlock, snapshotSlot, count.addAndGet(batchSize));

        // Update ExecutionContext with the new startOffset for the next chunk
        executionContext.putLong(START_OFFSET, Long.valueOf(to));

        boolean shouldContinue = to < finalEndOffset;

        return shouldContinue ? RepeatStatus.CONTINUABLE : RepeatStatus.FINISHED;
    }

    private void calculateAddressBalance(long from, long to, Long snapshotSlot, Long snapshotBlock, Long snapshotBlockTime, int snapshotEpoch) {
        var insertQuery = dsl.insertInto(ADDRESS_BALANCE,
                        ADDRESS_BALANCE.ADDRESS,
                        ADDRESS_BALANCE.UNIT,
                        ADDRESS_BALANCE.QUANTITY,
                        ADDRESS_BALANCE.SLOT,
                        ADDRESS_BALANCE.BLOCK,
                        ADDRESS_BALANCE.BLOCK_TIME,
                        ADDRESS_BALANCE.EPOCH,
                        ADDRESS_BALANCE.UPDATE_DATETIME
                ).select(
                        select(field(UTXO_AMOUNT.OWNER_ADDR).as("address"),
                                field(UTXO_AMOUNT.UNIT).as("unit"),
                                coalesce(sum(field(UTXO_AMOUNT.QUANTITY)), BigDecimal.ZERO).cast(SQLDataType.DECIMAL_INTEGER(38)).as("quantity"),
                                val(snapshotSlot),
                                val(snapshotBlock),
                                val(snapshotBlockTime),
                                val(snapshotEpoch),
                                currentLocalDateTime()
                        )
                                .from(UTXO_AMOUNT)
                                .leftJoin(TX_INPUT)
                                    .on(UTXO_AMOUNT.TX_HASH.eq(TX_INPUT.TX_HASH).and(UTXO_AMOUNT.OUTPUT_INDEX.eq(TX_INPUT.OUTPUT_INDEX)))
                                .where(UTXO_AMOUNT.SLOT.le(snapshotSlot)
                                        .and(UTXO_AMOUNT.OWNER_ADDR
                                                .in(select(ADDRESS.ADDRESS_)
                                                        .from(ADDRESS)
                                                        .where(ADDRESS.ID.between(from, to))
                                                        .limit(to - from + 1)
                                                )
                                        )
                                        .and(TX_INPUT.TX_HASH.isNull().or(TX_INPUT.SPENT_AT_SLOT.gt(snapshotSlot)))
                                )
                                .groupBy(UTXO_AMOUNT.OWNER_ADDR, UTXO_AMOUNT.UNIT)
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

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.execute(status -> {
            insertQuery.queryTimeout(300).execute();
            return true;
        });
    }

}


