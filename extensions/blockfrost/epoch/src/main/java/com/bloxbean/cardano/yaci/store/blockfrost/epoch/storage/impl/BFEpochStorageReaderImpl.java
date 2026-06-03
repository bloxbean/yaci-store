package com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.BFEpochStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SortField;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.blocks.jooq.Tables.BLOCK;
import static com.bloxbean.cardano.yaci.store.epoch_aggr.jooq.Tables.EPOCH;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;
import static org.jooq.impl.DSL.sum;

@Component
@RequiredArgsConstructor
public class BFEpochStorageReaderImpl implements BFEpochStorageReader {
    private final DSLContext dsl;

    @Override
    public List<Epoch> findNextEpochs(int epoch, int page, int count) {
        int offset = Math.max(page, 0) * count;

        return dsl.select(EPOCH.NUMBER, EPOCH.BLOCK_COUNT, EPOCH.TRANSACTION_COUNT, EPOCH.TOTAL_OUTPUT,
                        EPOCH.TOTAL_FEES, EPOCH.START_TIME, EPOCH.END_TIME, EPOCH.MAX_SLOT)
                .from(EPOCH)
                .where(EPOCH.NUMBER.gt((long) epoch))
                .orderBy(EPOCH.NUMBER.asc())
                .limit(count)
                .offset(offset)
                .fetch(this::toEpoch);
    }

    @Override
    public List<Epoch> findPreviousEpochs(int epoch, int page, int count) {
        int offset = Math.max(page, 0) * count;

        // Select N nearest predecessors (DESC), then return in ascending order to match Blockfrost spec
        var subSelect = dsl.select(EPOCH.NUMBER)
                .from(EPOCH)
                .where(EPOCH.NUMBER.lt((long) epoch))
                .orderBy(EPOCH.NUMBER.desc())
                .limit(count)
                .offset(offset);

        return dsl.select(EPOCH.NUMBER, EPOCH.BLOCK_COUNT, EPOCH.TRANSACTION_COUNT, EPOCH.TOTAL_OUTPUT,
                        EPOCH.TOTAL_FEES, EPOCH.START_TIME, EPOCH.END_TIME, EPOCH.MAX_SLOT)
                .from(EPOCH)
                .where(EPOCH.NUMBER.in(subSelect))
                .orderBy(EPOCH.NUMBER.asc())
                .fetch(this::toEpoch);
    }

    @Override
    public List<String> findBlockHashesByEpoch(int epoch, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        SortField<?> orderBy = order == Order.desc ? BLOCK.SLOT.desc() : BLOCK.SLOT.asc();

        return dsl.select(BLOCK.HASH)
                .from(BLOCK)
                .where(BLOCK.EPOCH.eq(epoch))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(record -> record.get(BLOCK.HASH));
    }

    @Override
    public List<String> findBlockHashesByEpochAndPool(int epoch, String poolId, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        SortField<?> orderBy = order == Order.desc ? BLOCK.SLOT.desc() : BLOCK.SLOT.asc();

        return dsl.select(BLOCK.HASH)
                .from(BLOCK)
                .where(BLOCK.EPOCH.eq(epoch))
                .and(BLOCK.SLOT_LEADER.eq(poolId))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(record -> record.get(BLOCK.HASH));
    }

    @Override
    public Map<Integer, BigInteger> getActiveStakesByEpochs(List<Integer> activeEpochs) {
        if (activeEpochs == null || activeEpochs.isEmpty()) {
            return Collections.emptyMap();
        }

        // active_epoch = epoch + 2 (invariant; set by StakeSnapshotService).
        // epoch_stake is partitioned by `epoch`, so filter on the partition key
        // instead of active_epoch — this enables partition pruning and reuse of
        // the PK (epoch, address), avoiding a full scan across all partitions.
        List<Integer> snapshotEpochs = activeEpochs.stream()
                .map(e -> e - 2)
                .toList();

        var totalStake = sum(EPOCH_STAKE.AMOUNT).cast(BigInteger.class).as("total_stake");
        return dsl.select(EPOCH_STAKE.EPOCH, totalStake)
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.EPOCH.in(snapshotEpochs))
                .groupBy(EPOCH_STAKE.EPOCH)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        r -> r.get(EPOCH_STAKE.EPOCH) + 2,   // remap partition epoch -> active_epoch
                        r -> r.get(totalStake)
                ));
    }

    private Epoch toEpoch(Record record) {
        Long number = record.get(EPOCH.NUMBER);
        if (number == null) {
            return null;
        }

        return Epoch.builder()
                .number(number)
                .blockCount(valueOrZero(record.get(EPOCH.BLOCK_COUNT)))
                .transactionCount(valueOrZero(record.get(EPOCH.TRANSACTION_COUNT)))
                .totalOutput(record.get(EPOCH.TOTAL_OUTPUT))
                .totalFees(toBigInteger(record.get(EPOCH.TOTAL_FEES)))
                .startTime(valueOrZero(record.get(EPOCH.START_TIME)))
                .endTime(valueOrZero(record.get(EPOCH.END_TIME)))
                .maxSlot(valueOrZero(record.get(EPOCH.MAX_SLOT)))
                .build();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private BigInteger toBigInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        if (value instanceof Number) {
            return BigInteger.valueOf(((Number) value).longValue());
        }
        return new BigInteger(value.toString());
    }
}
