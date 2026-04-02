package com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.BFBlocksStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockAddressTxRow;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockRow;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockTxCborRow;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.SelectConditionStep;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.blocks.jooq.Tables.BLOCK;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION_CBOR;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

@Component
@RequiredArgsConstructor
public class BFBlocksStorageReaderImpl implements BFBlocksStorageReader {
    private final DSLContext dsl;

    @Override
    public Optional<BFBlockRow> findLatestBlock() {
        var block = BLOCK.as("b");
        var nextBlockField = nextBlockField(block);
        var confirmationsField = confirmationsField(block);

        return blockBaseSelect(block, nextBlockField, confirmationsField, DSL.trueCondition())
                .orderBy(block.NUMBER.desc().nullsLast())
                .limit(1)
                .fetchOptional(record -> toBlockRow(record, block, nextBlockField, confirmationsField));
    }

    @Override
    public Optional<BFBlockRow> findBlockByHash(String hash) {
        var block = BLOCK.as("b");
        var nextBlockField = nextBlockField(block);
        var confirmationsField = confirmationsField(block);

        return blockBaseSelect(block, nextBlockField, confirmationsField, block.HASH.eq(hash))
                .limit(1)
                .fetchOptional(record -> toBlockRow(record, block, nextBlockField, confirmationsField));
    }

    @Override
    public Optional<BFBlockRow> findBlockByNumber(long number) {
        var block = BLOCK.as("b");
        var nextBlockField = nextBlockField(block);
        var confirmationsField = confirmationsField(block);

        return blockBaseSelect(block, nextBlockField, confirmationsField, block.NUMBER.eq(number))
                .limit(1)
                .fetchOptional(record -> toBlockRow(record, block, nextBlockField, confirmationsField));
    }

    @Override
    public Optional<BFBlockRow> findBlockBySlot(long slot) {
        var block = BLOCK.as("b");
        var nextBlockField = nextBlockField(block);
        var confirmationsField = confirmationsField(block);

        return blockBaseSelect(block, nextBlockField, confirmationsField, block.SLOT.eq(slot))
                .orderBy(block.NUMBER.desc().nullsLast())
                .limit(1)
                .fetchOptional(record -> toBlockRow(record, block, nextBlockField, confirmationsField));
    }

    @Override
    public Optional<BFBlockRow> findBlockByEpochAndEpochSlot(int epoch, int epochSlot) {
        var block = BLOCK.as("b");
        var nextBlockField = nextBlockField(block);
        var confirmationsField = confirmationsField(block);

        return blockBaseSelect(
                block,
                nextBlockField,
                confirmationsField,
                block.EPOCH.eq(epoch).and(block.EPOCH_SLOT.eq(epochSlot))
        )
                .orderBy(block.NUMBER.desc().nullsLast())
                .limit(1)
                .fetchOptional(record -> toBlockRow(record, block, nextBlockField, confirmationsField));
    }

    @Override
    public List<BFBlockRow> findNextBlocks(long blockNumber, int page, int count) {
        int offset = Math.max(page, 0) * count;

        var block = BLOCK.as("b");
        var nextBlockField = nextBlockField(block);
        var confirmationsField = confirmationsField(block);

        return blockBaseSelect(block, nextBlockField, confirmationsField, block.NUMBER.gt(blockNumber))
                .orderBy(block.NUMBER.asc().nullsLast())
                .limit(count)
                .offset(offset)
                .fetch(record -> toBlockRow(record, block, nextBlockField, confirmationsField));
    }

    @Override
    public List<BFBlockRow> findPreviousBlocks(long blockNumber, int page, int count) {
        int offset = Math.max(page, 0) * count;

        var block = BLOCK.as("b");
        var nextBlockField = nextBlockField(block);
        var confirmationsField = confirmationsField(block);

        // Query desc to get blocks immediately preceding blockNumber first (correct pagination),
        // then reverse to return in ascending order matching Blockfrost convention.
        List<BFBlockRow> results = new ArrayList<>(
                blockBaseSelect(block, nextBlockField, confirmationsField, block.NUMBER.lt(blockNumber))
                        .orderBy(block.NUMBER.desc().nullsLast())
                        .limit(count)
                        .offset(offset)
                        .fetch(record -> toBlockRow(record, block, nextBlockField, confirmationsField))
        );
        Collections.reverse(results);
        return results;
    }

    @Override
    public List<String> findBlockTxHashes(long blockNumber, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;

        var transaction = TRANSACTION.as("tx");
        SortField<?> txIndexOrder = order == Order.desc
                ? transaction.TX_INDEX.desc().nullsLast()
                : transaction.TX_INDEX.asc().nullsFirst();
        SortField<?> txHashOrder = order == Order.desc
                ? transaction.TX_HASH.desc()
                : transaction.TX_HASH.asc();

        return dsl.select(transaction.TX_HASH)
                .from(transaction)
                .where(transaction.BLOCK.eq(blockNumber))
                .orderBy(txIndexOrder, txHashOrder)
                .limit(count)
                .offset(offset)
                .fetchInto(String.class);
    }

    @Override
    public List<BFBlockTxCborRow> findBlockTxCbor(long blockNumber, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;

        var transaction = TRANSACTION.as("tx");
        var transactionCbor = TRANSACTION_CBOR.as("tx_cbor");

        SortField<?> txIndexOrder = order == Order.desc
                ? transaction.TX_INDEX.desc().nullsLast()
                : transaction.TX_INDEX.asc().nullsFirst();
        SortField<?> txHashOrder = order == Order.desc
                ? transaction.TX_HASH.desc()
                : transaction.TX_HASH.asc();

        return dsl.select(
                        transaction.TX_HASH,
                        transactionCbor.CBOR_DATA,
                        transaction.TX_INDEX
                )
                .from(transaction)
                .join(transactionCbor)
                .on(transactionCbor.TX_HASH.eq(transaction.TX_HASH))
                .where(transaction.BLOCK.eq(blockNumber))
                .orderBy(txIndexOrder, txHashOrder)
                .limit(count)
                .offset(offset)
                .fetch(record -> new BFBlockTxCborRow(
                        record.get(transaction.TX_HASH),
                        record.get(transactionCbor.CBOR_DATA),
                        record.get(transaction.TX_INDEX)
                ));
    }

    @Override
    public List<BFBlockAddressTxRow> findBlockAddressTransactions(long blockNumber, int page, int count) {
        int offset = Math.max(page, 0) * count;

        var outputUtxo = ADDRESS_UTXO.as("output_utxo");
        Field<String> outputAddressExpr = DSL.coalesce(outputUtxo.OWNER_ADDR_FULL, outputUtxo.OWNER_ADDR);
        Field<String> outputAddressField = outputAddressExpr.as("address");

        var outputsSelect = dsl.select(outputAddressField, outputUtxo.TX_HASH.as("tx_hash"))
                .from(outputUtxo)
                .where(outputUtxo.BLOCK.eq(blockNumber))
                .and(outputAddressExpr.isNotNull());

        var spentInput = TX_INPUT.as("spent_input");
        var spentUtxo = ADDRESS_UTXO.as("spent_utxo");
        Field<String> spentAddressExpr = DSL.coalesce(spentUtxo.OWNER_ADDR_FULL, spentUtxo.OWNER_ADDR);
        Field<String> spentAddressField = spentAddressExpr.as("address");

        var spentSelect = dsl.select(spentAddressField, spentInput.SPENT_TX_HASH.as("tx_hash"))
                .from(spentInput)
                .join(spentUtxo)
                .on(spentInput.TX_HASH.eq(spentUtxo.TX_HASH))
                .and(spentInput.OUTPUT_INDEX.eq(spentUtxo.OUTPUT_INDEX))
                .where(spentInput.SPENT_AT_BLOCK.eq(blockNumber))
                .and(spentInput.SPENT_TX_HASH.isNotNull())
                .and(spentAddressExpr.isNotNull());

        Table<?> affected = outputsSelect.unionAll(spentSelect).asTable("affected");
        Field<String> affectedAddressField = affected.field("address", String.class);
        Field<String> affectedTxHashField = affected.field("tx_hash", String.class);

        if (affectedAddressField == null || affectedTxHashField == null) {
            return List.of();
        }

        Table<?> pagedAddresses = dsl.selectDistinct(affectedAddressField.as("address"))
                .from(affected)
                .where(affectedAddressField.isNotNull())
                .orderBy(affectedAddressField.asc())
                .limit(count)
                .offset(offset)
                .asTable("paged_addresses");

        Field<String> pagedAddressField = pagedAddresses.field("address", String.class);
        if (pagedAddressField == null) {
            return List.of();
        }

        var transaction = TRANSACTION.as("tx");
        Field<Integer> txIndexField = DSL.min(transaction.TX_INDEX).as("tx_index");

        return dsl.select(
                        pagedAddressField.as("address"),
                        affectedTxHashField.as("tx_hash"),
                        txIndexField
                )
                .from(pagedAddresses)
                .join(affected)
                .on(affectedAddressField.eq(pagedAddressField))
                .leftJoin(transaction)
                .on(transaction.TX_HASH.eq(affectedTxHashField))
                .and(transaction.BLOCK.eq(blockNumber))
                .where(affectedTxHashField.isNotNull())
                .groupBy(pagedAddressField, affectedTxHashField)
                .orderBy(pagedAddressField.asc(), txIndexField.asc().nullsFirst(), affectedTxHashField.asc())
                .fetch(record -> new BFBlockAddressTxRow(
                        record.get(pagedAddressField),
                        record.get(affectedTxHashField),
                        record.get(txIndexField)
                ));
    }

    private SelectConditionStep<? extends Record> blockBaseSelect(
            com.bloxbean.cardano.yaci.store.blocks.jooq.tables.Block block,
            Field<String> nextBlockField,
            Field<Long> confirmationsField,
            Condition condition
    ) {
        return dsl.select(
                        block.BLOCK_TIME,
                        block.NUMBER,
                        block.HASH,
                        block.SLOT,
                        block.EPOCH,
                        block.EPOCH_SLOT,
                        block.SLOT_LEADER,
                        block.BODY_SIZE,
                        block.NO_OF_TXS,
                        block.TOTAL_OUTPUT,
                        block.TOTAL_FEES,
                        block.VRF_VKEY,
                        block.OP_CERT_HOT_VKEY,
                        block.OP_CERT_SEQ_NUMBER,
                        block.PREV_HASH,
                        nextBlockField,
                        confirmationsField
                )
                .from(block)
                .where(condition);
    }

    private Field<String> nextBlockField(com.bloxbean.cardano.yaci.store.blocks.jooq.tables.Block block) {
        var nextBlock = BLOCK.as("next_b");
        return DSL.select(nextBlock.HASH)
                .from(nextBlock)
                .where(nextBlock.NUMBER.gt(block.NUMBER))
                .orderBy(nextBlock.NUMBER.asc())
                .limit(1)
                .asField("next_block");
    }

    private Field<Long> confirmationsField(com.bloxbean.cardano.yaci.store.blocks.jooq.tables.Block block) {
        var latestBlock = BLOCK.as("latest_b");
        Field<Long> latestBlockNumber = DSL.select(DSL.max(latestBlock.NUMBER))
                .from(latestBlock)
                .asField();

        return DSL.coalesce(latestBlockNumber.minus(block.NUMBER), DSL.inline(0L)).as("confirmations");
    }

    private BFBlockRow toBlockRow(
            Record record,
            com.bloxbean.cardano.yaci.store.blocks.jooq.tables.Block block,
            Field<String> nextBlockField,
            Field<Long> confirmationsField
    ) {
        Long bodySize = record.get(block.BODY_SIZE);
        Integer txCount = record.get(block.NO_OF_TXS);
        boolean hasTransactions = txCount != null && txCount > 0;

        return new BFBlockRow(
                record.get(block.BLOCK_TIME),
                record.get(block.NUMBER),
                record.get(block.HASH),
                record.get(block.SLOT),
                record.get(block.EPOCH),
                record.get(block.EPOCH_SLOT),
                record.get(block.SLOT_LEADER),
                bodySize,
                txCount,
                hasTransactions ? toBigInteger(record.get(block.TOTAL_OUTPUT)) : null,
                hasTransactions ? toBigInteger(record.get(block.TOTAL_FEES)) : null,
                record.get(block.VRF_VKEY),
                record.get(block.OP_CERT_HOT_VKEY),
                record.get(block.OP_CERT_SEQ_NUMBER),
                record.get(block.PREV_HASH),
                record.get(nextBlockField),
                record.get(confirmationsField)
        );
    }

    private BigInteger toBigInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.toBigInteger();
        }
        if (value instanceof Number number) {
            return BigInteger.valueOf(number.longValue());
        }

        return new BigInteger(value.toString());
    }
}
