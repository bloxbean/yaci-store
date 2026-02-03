package com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.BFAddressStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl.model.BFAddressTotal;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.AddressUtil;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Record2;
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

import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.TRANSACTION;
import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.TX_INPUT;
import static com.bloxbean.cardano.yaci.store.blockfrost_address.jooq.Tables.ADDRESS_BALANCE_CURRENT;

@Component
@RequiredArgsConstructor
@Slf4j
public class BFAddressStorageReaderImpl implements BFAddressStorageReader {
    private final DSLContext dsl;

    /**
     * Find transaction hashes for an address with pagination and ordering.
     *
     * @param address Bech32 address.
     * @param page zero-based page index.
     * @param count page size.
     * @param order sort order by slot.
     * @return distinct transaction hashes for the address.
     */
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

    /**
     * Find transactions for an address with pagination, ordering, and optional block range.
     *
     * @param address Bech32 address.
     * @param page zero-based page index.
     * @param count page size.
     * @param order sort order by block height and tx index.
     * @param from inclusive block reference ("block[:txIndex]") or null.
     * @param to inclusive block reference ("block[:txIndex]") or null.
     * @return list of transactions for the address.
     */
    @Override
    public List<BFAddressTransactionDTO> findAddressTransactions(String address, int page, int count, Order order, String from, String to) {
        int offset = Math.max(page, 0) * count;
        Table<?> combinedTx = buildAddressTxTable(address);
        Field<String> txHashField = combinedTx.field("tx_hash", String.class);

        BlockRef fromRef = parseBlockRef(from);
        BlockRef toRef = parseBlockRef(to);
        Condition rangeCondition = buildRangeCondition(fromRef, toRef);

        Field<Integer> txIndexField = DSL.coalesce(TRANSACTION.TX_INDEX, 0);
        Field<Long> txIndexSelect = txIndexField.cast(Long.class).as("txIndex");
        Field<Long> blockField = TRANSACTION.BLOCK.as("blockHeight");
        SortField<?> blockOrder = order == Order.desc ? blockField.desc() : blockField.asc();
        SortField<?> txIndexOrder = order == Order.desc ? txIndexSelect.desc() : txIndexSelect.asc();

        return dsl.selectDistinct(
                TRANSACTION.TX_HASH.as("txHash"),
                txIndexSelect,
                blockField,
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

    /**
     * Get received/sent totals and transaction count for an address.
     *
     * @param address Bech32 address.
     * @return totals and tx count, empty if the query fails.
     */
    @Override
    public Optional<BFAddressTotal> getAddressTotal(String address) {
        try {
            Condition condition = addressCondition(address, ADDRESS_UTXO.OWNER_ADDR, ADDRESS_UTXO.OWNER_ADDR_FULL);

            Map<String, BigInteger> receivedMap = fetchAmountSums(condition, false);
            Map<String, BigInteger> sentMap = fetchAmountSums(condition, true);

            Table<?> combinedTx = buildAddressTxTable(address);
            Long txCount = dsl.select(DSL.countDistinct(DSL.field("tx_hash", String.class)))
                    .from(combinedTx)
                    .fetchOne(0, Long.class);

            long count = txCount == null ? 0L : txCount;
            return Optional.of(new BFAddressTotal(receivedMap, sentMap, count));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Return current balance by unit from the account aggregate table.
     *
     * @param address Bech32 address.
     * @return map of unit to quantity.
     */
    @Override
    public Map<String, BigInteger> findCurrentAddressBalanceByUnit(String address) {
        Condition condition = addressCondition(address, ADDRESS_BALANCE_CURRENT.ADDRESS, ADDRESS_BALANCE_CURRENT.ADDR_FULL);

        return dsl.select(ADDRESS_BALANCE_CURRENT.UNIT, ADDRESS_BALANCE_CURRENT.QUANTITY)
                .from(ADDRESS_BALANCE_CURRENT)
                .where(condition)
                .fetchMap(ADDRESS_BALANCE_CURRENT.UNIT, ADDRESS_BALANCE_CURRENT.QUANTITY);
    }

    /**
     * Return unspent balance by unit for an address.
     *
     * @param address Bech32 address.
     * @return map of unit to quantity.
     */
    @Override
    public Map<String, BigInteger> findUnspentAddressBalanceByUnit(String address) {
        Condition condition = addressCondition(address, ADDRESS_UTXO.OWNER_ADDR, ADDRESS_UTXO.OWNER_ADDR_FULL);
        return fetchUnspentAmountSums(condition);
    }

    /**
     * Aggregate amounts for an address. When {@code spent} is true, sum amounts of spent outputs;
     * otherwise sum received amounts. Lovelace is sourced from {@code lovelace_amount} to avoid
     * JSON expansion where possible, and JSON amounts are used for non-lovelace assets.
     */
    private Map<String, BigInteger> fetchAmountSums(Condition addressCondition, boolean spent) {
        Select<?> base = spent
                ? dsl.select(
                            ADDRESS_UTXO.TX_HASH.as("tx_hash"),
                            ADDRESS_UTXO.OUTPUT_INDEX.as("output_index"),
                            ADDRESS_UTXO.AMOUNTS.as("amounts"),
                            ADDRESS_UTXO.LOVELACE_AMOUNT.as("lovelace_amount")
                    )
                    .from(ADDRESS_UTXO)
                    .join(TX_INPUT)
                    .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                    .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                    .where(addressCondition)
                : dsl.select(
                            ADDRESS_UTXO.TX_HASH.as("tx_hash"),
                            ADDRESS_UTXO.OUTPUT_INDEX.as("output_index"),
                            ADDRESS_UTXO.AMOUNTS.as("amounts"),
                            ADDRESS_UTXO.LOVELACE_AMOUNT.as("lovelace_amount")
                    )
                    .from(ADDRESS_UTXO)
                    .where(addressCondition);

        Table<?> baseTable = base.asTable("base");
        Field<?> amountsField = DSL.field(DSL.name("base", "amounts"));
        Field<BigDecimal> lovelaceAmountField = DSL.field(DSL.name("base", "lovelace_amount"), BigDecimal.class);

        Select<Record2<String, BigDecimal>> lovelaceSelect = dsl.select(
                        DSL.inline("lovelace").as("unit"),
                        DSL.sum(lovelaceAmountField).cast(BigDecimal.class).as("quantity")
                )
                .from(baseTable)
                .where(lovelaceAmountField.isNotNull());

        Table<?> amountTable = DSL.table("jsonb_to_recordset({0}::jsonb) as amt(unit text, quantity numeric)", amountsField);
        Field<String> unitField = DSL.field("amt.unit", String.class);
        Field<BigDecimal> quantityField = DSL.field("amt.quantity", BigDecimal.class);
        Field<BigDecimal> quantitySum = DSL.sum(quantityField).as("quantity");

        Select<Record2<String, BigDecimal>> assetsSelect = dsl.select(unitField.as("unit"), quantitySum.as("quantity"))
                .from(baseTable)
                .join(DSL.lateral(amountTable)).on(DSL.trueCondition())
                .where(amountsField.isNotNull())
                .and(unitField.ne("lovelace")
                        .or(lovelaceAmountField.isNull()
                                .or(lovelaceAmountField.eq(BigDecimal.ZERO))
                                .and(unitField.eq("lovelace"))))
                .groupBy(unitField);

        Table<?> combined = lovelaceSelect.unionAll(assetsSelect).asTable("combined");
        Field<String> combinedUnit = combined.field("unit", String.class);
        Field<BigDecimal> combinedQuantity = combined.field("quantity", BigDecimal.class);
        Field<BigDecimal> combinedSum = DSL.sum(combinedQuantity).as("quantity");

        var finalQuery = dsl.select(combinedUnit, combinedSum)
                .from(combined)
                .groupBy(combinedUnit);

        return finalQuery.fetchMap(combinedUnit, combinedSum)
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().toBigInteger()), HashMap::putAll);
    }

    /**
     * Aggregate unspent amounts for an address using an anti-join to exclude spent outputs.
     * Lovelace is sourced from {@code lovelace_amount}; JSON is used for non-lovelace assets,
     * and for lovelace when the scalar amount is missing or zero.
     */
    private Map<String, BigInteger> fetchUnspentAmountSums(Condition addressCondition) {
        var unspentCte = DSL.name("unspent").as(
                dsl.select(
                                ADDRESS_UTXO.TX_HASH.as("tx_hash"),
                                ADDRESS_UTXO.OUTPUT_INDEX.as("output_index"),
                                ADDRESS_UTXO.AMOUNTS.as("amounts"),
                                ADDRESS_UTXO.LOVELACE_AMOUNT.as("lovelace_amount")
                        )
                        .from(ADDRESS_UTXO)
                        .where(addressCondition)
                        .and(DSL.notExists(
                                dsl.selectOne()
                                        .from(TX_INPUT)
                                        .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                                        .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                        ))
        );

        Table<?> unspentTable = DSL.table(DSL.name("unspent"));
        Field<?> amountsField = DSL.field(DSL.name("unspent", "amounts"));
        Field<BigDecimal> lovelaceAmountField = DSL.field(DSL.name("unspent", "lovelace_amount"), BigDecimal.class);
        Table<?> amountTable = DSL.table("jsonb_to_recordset({0}::jsonb) as amt(unit text, quantity numeric)", amountsField);
        Field<String> unitField = DSL.field("amt.unit", String.class);
        Field<BigDecimal> quantityField = DSL.field("amt.quantity", BigDecimal.class);
        Field<BigDecimal> quantitySum = DSL.sum(quantityField).as("quantity");

        Select<Record2<String, BigDecimal>> lovelaceSelect = dsl.select(
                        DSL.inline("lovelace").as("unit"),
                        DSL.sum(lovelaceAmountField).cast(BigDecimal.class).as("quantity")
                )
                .from(unspentTable)
                .where(lovelaceAmountField.isNotNull());

        Select<Record2<String, BigDecimal>> assetsSelect = dsl.select(unitField.as("unit"), quantitySum.as("quantity"))
                .from(unspentTable)
                .join(DSL.lateral(amountTable)).on(DSL.trueCondition())
                .where(amountsField.isNotNull())
                .and(unitField.ne("lovelace")
                        .or(lovelaceAmountField.isNull()
                                .or(lovelaceAmountField.eq(BigDecimal.ZERO))
                                .and(unitField.eq("lovelace"))))
                .groupBy(unitField);

        Table<?> combined = lovelaceSelect.unionAll(assetsSelect).asTable("combined");
        Field<String> combinedUnit = combined.field("unit", String.class);
        Field<BigDecimal> combinedQuantity = combined.field("quantity", BigDecimal.class);
        Field<BigDecimal> combinedSum = DSL.sum(combinedQuantity).as("quantity");

        var finalQuery = dsl.with(unspentCte)
                .select(combinedUnit, combinedSum)
                .from(combined)
                .groupBy(combinedUnit);

        return finalQuery.fetchMap(combinedUnit, combinedSum)
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().toBigInteger()), HashMap::putAll);
    }

    /**
     * Build a combined transaction set for an address from both outputs and spent inputs.
     */
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
    /**
     * Build address matching condition, including owner_addr_full for long Byron addresses.
     */
    private Condition addressCondition(String address, TableField<?, String> addressField, TableField<?, String> addressFullField) {
        Tuple<String, String> addressTuple = AddressUtil.getAddress(address);
        if (addressTuple._2 != null) {
            return addressField.eq(addressTuple._1)
                    .and(addressFullField.eq(addressTuple._2));
        }
        return addressField.eq(addressTuple._1);
    }

    /**
     * Parse a block reference in the form "block[:txIndex]".
     */
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

    /**
     * Build an inclusive block range condition on TRANSACTION.BLOCK and TRANSACTION.TX_INDEX.
     */
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
