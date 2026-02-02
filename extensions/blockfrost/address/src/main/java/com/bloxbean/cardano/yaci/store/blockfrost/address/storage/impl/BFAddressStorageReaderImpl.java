package com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.BFAddressStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl.model.BFAddressTotal;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.AddressUtil;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.Select;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.ADDRESS_TX_AMOUNT;
import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.TRANSACTION;
import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.TX_INPUT;
import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.ADDRESS_BALANCE;

@Component
@RequiredArgsConstructor
public class BFAddressStorageReaderImpl implements BFAddressStorageReader {
    private final DSLContext dsl;

    @Override
    public List<String> findTxHashesByAddress(String address, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        Table<?> combinedTx = buildAddressTxTable(address);

        SortField<?> orderBy = order == Order.desc
                ? DSL.field("slot").desc()
                : DSL.field("slot").asc();

        return dsl.selectDistinct(DSL.field("tx_hash", String.class))
                .from(combinedTx)
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetchInto(String.class);
    }

    @Override
    public List<BFAddressTransactionDTO> findAddressTransactions(String address, int page, int count, Order order, String from, String to) {
        int offset = Math.max(page, 0) * count;
        Table<?> combinedTx = buildAddressTxTable(address);
        Field<String> txHashField = combinedTx.field("tx_hash", String.class);

        BlockRef fromRef = parseBlockRef(from);
        BlockRef toRef = parseBlockRef(to);
        Condition rangeCondition = buildRangeCondition(fromRef, toRef);

        Field<Integer> txIndexField = DSL.coalesce(TRANSACTION.TX_INDEX, 0);
        SortField<?> blockOrder = order == Order.desc ? TRANSACTION.BLOCK.desc() : TRANSACTION.BLOCK.asc();
        SortField<?> txIndexOrder = order == Order.desc ? txIndexField.desc() : txIndexField.asc();

        return dsl.selectDistinct(
                        TRANSACTION.TX_HASH.as("txHash"),
                        txIndexField.cast(Long.class).as("txIndex"),
                        TRANSACTION.BLOCK.as("blockHeight"),
                        TRANSACTION.BLOCK_TIME.as("blockTime")
                )
                .from(combinedTx)
                .join(TRANSACTION)
                .on(TRANSACTION.TX_HASH.eq(txHashField))
                .where(rangeCondition)
                .orderBy(blockOrder, txIndexOrder)
                .limit(count)
                .offset(offset)
                .fetchInto(BFAddressTransactionDTO.class);
    }

    @Override
    public Optional<BFAddressTotal> getAddressTotal(String address) {
        try {
            Condition condition = addressCondition(address, ADDRESS_TX_AMOUNT.ADDRESS, ADDRESS_TX_AMOUNT.ADDR_FULL);

            Field<BigDecimal> receivedSum = DSL.sum(DSL.when(ADDRESS_TX_AMOUNT.QUANTITY.gt(BigInteger.ZERO), ADDRESS_TX_AMOUNT.QUANTITY)
                    .otherwise(BigInteger.ZERO)).as("received_sum");
            Field<BigDecimal> sentSum = DSL.sum(DSL.when(ADDRESS_TX_AMOUNT.QUANTITY.lt(BigInteger.ZERO), ADDRESS_TX_AMOUNT.QUANTITY.neg())
                    .otherwise(BigInteger.ZERO)).as("sent_sum");

            var records = dsl.select(ADDRESS_TX_AMOUNT.UNIT, receivedSum, sentSum)
                    .from(ADDRESS_TX_AMOUNT)
                    .where(condition)
                    .groupBy(ADDRESS_TX_AMOUNT.UNIT)
                    .fetch();

            Map<String, BigInteger> receivedMap = new HashMap<>();
            Map<String, BigInteger> sentMap = new HashMap<>();

            for (var record : records) {
                String unit = record.get(ADDRESS_TX_AMOUNT.UNIT);
                BigDecimal received = record.get(receivedSum);
                BigDecimal sent = record.get(sentSum);

                if (received != null && received.compareTo(BigDecimal.ZERO) != 0) {
                    receivedMap.put(unit, received.toBigInteger());
                }
                if (sent != null && sent.compareTo(BigDecimal.ZERO) != 0) {
                    sentMap.put(unit, sent.toBigInteger());
                }
            }

            Long txCount = dsl.select(DSL.countDistinct(ADDRESS_TX_AMOUNT.TX_HASH))
                    .from(ADDRESS_TX_AMOUNT)
                    .where(condition)
                    .fetchOne(0, Long.class);

            long count = txCount == null ? 0L : txCount;
            return Optional.of(new BFAddressTotal(receivedMap, sentMap, count));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, BigInteger> findLatestAddressBalanceByUnit(String address) {
        Condition condition = addressCondition(address, ADDRESS_BALANCE.ADDRESS, ADDRESS_BALANCE.ADDR_FULL);

        var latestSlot = DSL.max(ADDRESS_BALANCE.SLOT).as("latest_slot");
        var latestPerUnit = dsl.select(ADDRESS_BALANCE.UNIT, latestSlot)
                .from(ADDRESS_BALANCE)
                .where(condition)
                .groupBy(ADDRESS_BALANCE.UNIT)
                .asTable("latest_per_unit");

        Field<String> unitField = latestPerUnit.field(ADDRESS_BALANCE.UNIT);
        Field<Long> slotField = latestPerUnit.field("latest_slot", Long.class);

        return dsl.select(ADDRESS_BALANCE.UNIT, ADDRESS_BALANCE.QUANTITY)
                .from(ADDRESS_BALANCE)
                .join(latestPerUnit)
                .on(ADDRESS_BALANCE.UNIT.eq(unitField))
                .and(ADDRESS_BALANCE.SLOT.eq(slotField))
                .where(condition)
                .fetchMap(ADDRESS_BALANCE.UNIT, ADDRESS_BALANCE.QUANTITY);
    }

    private Table<?> buildAddressTxTable(String address) {
        Condition addressCondition = addressCondition(address, ADDRESS_UTXO.OWNER_ADDR, ADDRESS_UTXO.OWNER_ADDR_FULL);

        Select<Record4<String, Long, Long, Long>> addressUtxoTx = dsl
                .select(ADDRESS_UTXO.TX_HASH.as("tx_hash"),
                        ADDRESS_UTXO.BLOCK.as("block"),
                        ADDRESS_UTXO.BLOCK_TIME.as("block_time"),
                        ADDRESS_UTXO.SLOT.as("slot"))
                .from(ADDRESS_UTXO)
                .where(addressCondition);

        Select<Record4<String, Long, Long, Long>> spentTx = dsl
                .select(TX_INPUT.SPENT_TX_HASH.as("tx_hash"),
                        TX_INPUT.SPENT_AT_BLOCK.as("block"),
                        TX_INPUT.SPENT_BLOCK_TIME.as("block_time"),
                        TX_INPUT.SPENT_AT_SLOT.as("slot"))
                .from(TX_INPUT)
                .join(ADDRESS_UTXO)
                .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(addressCondition);

        return addressUtxoTx.union(spentTx).asTable("combined_tx");
    }

    private Condition addressCondition(String address, TableField<?, String> addressField, TableField<?, String> addressFullField) {
        Tuple<String, String> addressTuple = AddressUtil.getAddress(address);
        if (addressTuple._2 != null) {
            return addressField.eq(addressTuple._1)
                    .and(addressFullField.eq(addressTuple._2));
        }
        return addressField.eq(addressTuple._1);
    }

    private BlockRef parseBlockRef(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(":", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("Invalid block reference: " + value);
        }
        long block;
        try {
            block = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid block reference: " + value, e);
        }
        if (block < 0) {
            throw new IllegalArgumentException("Invalid block reference: " + value);
        }

        Integer txIndex = null;
        if (parts.length == 2 && !parts[1].isBlank()) {
            try {
                txIndex = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid block reference: " + value, e);
            }
            if (txIndex < 0) {
                throw new IllegalArgumentException("Invalid block reference: " + value);
            }
        }

        return new BlockRef(block, txIndex);
    }

    private Condition buildRangeCondition(BlockRef from, BlockRef to) {
        Condition condition = DSL.trueCondition();
        if (from != null) {
            int fromIndex = from.txIndex == null ? 0 : from.txIndex;
            condition = condition.and(
                    TRANSACTION.BLOCK.gt(from.block)
                            .or(TRANSACTION.BLOCK.eq(from.block)
                                    .and(DSL.coalesce(TRANSACTION.TX_INDEX, 0).ge(fromIndex)))
            );
        }
        if (to != null) {
            int toIndex = to.txIndex == null ? Integer.MAX_VALUE : to.txIndex;
            condition = condition.and(
                    TRANSACTION.BLOCK.lt(to.block)
                            .or(TRANSACTION.BLOCK.eq(to.block)
                                    .and(DSL.coalesce(TRANSACTION.TX_INDEX, 0).le(toIndex)))
            );
        }
        return condition;
    }

    private record BlockRef(long block, Integer txIndex) {
    }
}
