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
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.STAKE_ADDRESS_BALANCE;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;
import static org.jooq.impl.DSL.*;

//TODO -- Not used
@RequiredArgsConstructor
@Slf4j
public class StakeAddressUtxoAggregationTasklet implements Tasklet {

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
            batchSize = finalEndOffset - startOffset;
        }

        log.info("<< Processing stake addresses from {}, batchSize: {}, to: {}, FinalEndOffSet {}, stepId: {} >>", startOffset, batchSize, to, finalEndOffset, chunkContext.getStepContext().getId());

        calculateStakeAddressBalance(startOffset, to, snapshotSlot, snapshotBlock, snapshotBlockTime, snapshotEpoch.intValue());

        log.info("<< Total stake addresses processed: {} >>", count.addAndGet(batchSize));

        // Update ExecutionContext with the new startOffset for the next chunk
        executionContext.putLong(START_OFFSET, Long.valueOf(to));

        boolean shouldContinue = to < finalEndOffset;

        if (!shouldContinue) {
            log.info("Stake address aggregation completed for partition. {}", chunkContext.getStepContext().getId());
        }

        return shouldContinue ? RepeatStatus.CONTINUABLE : RepeatStatus.FINISHED;
    }

    private void calculateStakeAddressBalance(long from, long to, Long snapshotSlot, Long snapshotBlock, Long snapshotBlockTime, int snapshotEpoch) {
        var insertQuery = dsl.insertInto(STAKE_ADDRESS_BALANCE,
                        STAKE_ADDRESS_BALANCE.ADDRESS,
                        STAKE_ADDRESS_BALANCE.QUANTITY,
                        STAKE_ADDRESS_BALANCE.SLOT,
                        STAKE_ADDRESS_BALANCE.BLOCK,
                        STAKE_ADDRESS_BALANCE.BLOCK_TIME,
                        STAKE_ADDRESS_BALANCE.EPOCH,
                        STAKE_ADDRESS_BALANCE.UPDATE_DATETIME
                ).select(
                        select(field(ADDRESS_UTXO.OWNER_STAKE_ADDR).as("address"),
                                coalesce(sum(field(ADDRESS_UTXO.LOVELACE_AMOUNT)), BigDecimal.ZERO).cast(SQLDataType.DECIMAL_INTEGER(38)).as("quantity"),
                                val(snapshotSlot),
                                val(snapshotBlock),
                                val(snapshotBlockTime),
                                val(snapshotEpoch),
                                currentLocalDateTime()
                        )
                                .from(ADDRESS_UTXO)
                                .leftJoin(TX_INPUT)
                                .on(ADDRESS_UTXO.TX_HASH.eq(TX_INPUT.TX_HASH).and(ADDRESS_UTXO.OUTPUT_INDEX.eq(TX_INPUT.OUTPUT_INDEX)))
                                .where(
                                        ADDRESS_UTXO.SLOT.le(snapshotSlot)
                                                .and(ADDRESS_UTXO.OWNER_STAKE_ADDR
                                                        .in(select(field("address", String.class))
                                                                .from(table(STAKE_ADDRESS_TEMP_TABLE))
                                                                .where(field("id", Long.class).between(from, to))
                                                                .limit(to - from + 1)
                                                        )
                                                )
                                                .and(TX_INPUT.TX_HASH.isNull().or(TX_INPUT.SPENT_AT_SLOT.gt(snapshotSlot)))
                                )
                                .groupBy(ADDRESS_UTXO.OWNER_STAKE_ADDR)
                )
                .onConflict(
                        STAKE_ADDRESS_BALANCE.ADDRESS,
                        STAKE_ADDRESS_BALANCE.SLOT
                )
                .doUpdate()
                .set(STAKE_ADDRESS_BALANCE.QUANTITY, excluded(STAKE_ADDRESS_BALANCE.QUANTITY))
                .set(STAKE_ADDRESS_BALANCE.SLOT, excluded(STAKE_ADDRESS_BALANCE.SLOT))
                .set(STAKE_ADDRESS_BALANCE.BLOCK, excluded(STAKE_ADDRESS_BALANCE.BLOCK))
                .set(STAKE_ADDRESS_BALANCE.BLOCK_TIME, excluded(STAKE_ADDRESS_BALANCE.BLOCK_TIME))
                .set(STAKE_ADDRESS_BALANCE.EPOCH, excluded(STAKE_ADDRESS_BALANCE.EPOCH));

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.execute(status -> {
            insertQuery.queryTimeout(300).execute();
            return null;
        });
    }

}


