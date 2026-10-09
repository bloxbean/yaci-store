package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectFieldOrAsterisk;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO_UNSPENT;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;
import static org.jooq.impl.DSL.row;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;

/**
 * Keeps {@code address_utxo_unspent} equal to {@code address_utxo} minus {@code tx_input}: the
 * outputs that are not spent yet, with every column of {@code address_utxo}.
 *
 * <p>It is maintained per <em>commit</em>, never per block, and only from rows already written to
 * {@code address_utxo} and {@code tx_input}:
 * <ol>
 *   <li>copy the outputs whose slot falls in the committed batch, unless already present;</li>
 *   <li>then delete the outputs whose {@code tx_input.spent_at_slot} falls in that batch.</li>
 * </ol>
 * Every batch before this one has already committed (the cursor moves only after the commit
 * listeners), so an output is always copied before its spend is applied. That holds whatever
 * order the blocks of a batch were written in, which matters because in parallel mode they are
 * written concurrently, and because {@code UtxoProcessor} saves a block's spends before its
 * outputs. Both steps are idempotent, so a batch replayed after a crash (its cursor never moved)
 * leaves the same rows.
 *
 * <p>The copy reads {@code address_utxo} rather than the in-memory outputs so it carries the
 * values written at commit time too (pointer-address stake fields, set in
 * {@code UtxoProcessor.handleCommit}, which is ordered before this).
 */
@Component
@Slf4j
public class UnspentUtxoTableService {

    private final DSLContext dsl;
    private final TransactionTemplate chunkTransaction;

    public UnspentUtxoTableService(DSLContext dsl, PlatformTransactionManager transactionManager) {
        this.dsl = dsl;
        this.chunkTransaction = new TransactionTemplate(transactionManager);
        this.chunkTransaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    /** The table's columns, in its own order. */
    private static Field<?>[] unspentColumns() {
        return ADDRESS_UTXO_UNSPENT.fields();
    }

    /** The same columns read from {@code address_utxo}, in {@link #unspentColumns()} order. */
    private static List<SelectFieldOrAsterisk> addressUtxoColumns() {
        return Arrays.stream(ADDRESS_UTXO_UNSPENT.fields())
                .map(f -> (SelectFieldOrAsterisk) ADDRESS_UTXO.field(f.getName()))
                .toList();
    }

    private static org.jooq.Condition notCopied() {
        return org.jooq.impl.DSL.notExists(selectOne().from(ADDRESS_UTXO_UNSPENT)
                .where(ADDRESS_UTXO_UNSPENT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                .and(ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)));
    }

    private static org.jooq.Condition notSpent() {
        return org.jooq.impl.DSL.notExists(selectOne().from(TX_INPUT)
                .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)));
    }

    /**
     * Applies one committed batch, the blocks between {@code fromSlot} and {@code toSlot}
     * inclusive.
     *
     * @return the rows copied in, and the rows deleted
     */
    @Transactional
    public int[] applyCommit(long fromSlot, long toSlot) {
        int copied = dsl.insertInto(ADDRESS_UTXO_UNSPENT, unspentColumns())
                .select(select(addressUtxoColumns())
                        .from(ADDRESS_UTXO)
                        .where(ADDRESS_UTXO.SLOT.between(fromSlot, toSlot))
                        .and(notCopied()))
                .execute();

        int deleted = deleteSpentBetween(fromSlot, toSlot);
        return new int[]{copied, deleted};
    }

    private int deleteSpentBetween(long fromSlot, Long toSlot) {
        var spent = select(TX_INPUT.TX_HASH, TX_INPUT.OUTPUT_INDEX).from(TX_INPUT)
                .where(toSlot == null
                        ? TX_INPUT.SPENT_AT_SLOT.ge(fromSlot)
                        : TX_INPUT.SPENT_AT_SLOT.between(fromSlot, toSlot));
        return dsl.deleteFrom(ADDRESS_UTXO_UNSPENT)
                .where(row(ADDRESS_UTXO_UNSPENT.TX_HASH, ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX).in(spent))
                .execute();
    }

    /**
     * Rolls the table back to {@code slot}. Must run before {@code UtxoRollbackProcessor} trims
     * {@code tx_input}: the outputs to restore are the ones whose spend is about to be removed.
     *
     * @return the rows deleted, and the rows restored
     */
    @Transactional
    public int[] rollbackTo(long slot) {
        int deleted = dsl.deleteFrom(ADDRESS_UTXO_UNSPENT)
                .where(ADDRESS_UTXO_UNSPENT.SLOT.gt(slot))
                .execute();

        int restored = dsl.insertInto(ADDRESS_UTXO_UNSPENT, unspentColumns())
                .select(select(addressUtxoColumns())
                        .from(TX_INPUT)
                        .join(ADDRESS_UTXO)
                        .on(ADDRESS_UTXO.TX_HASH.eq(TX_INPUT.TX_HASH))
                        .and(ADDRESS_UTXO.OUTPUT_INDEX.eq(TX_INPUT.OUTPUT_INDEX))
                        .where(TX_INPUT.SPENT_AT_SLOT.gt(slot))
                        .and(ADDRESS_UTXO.SLOT.le(slot))
                        .and(notCopied()))
                .execute();
        return new int[]{deleted, restored};
    }

    /**
     * One-time fill for a store that was synced before the table was enabled: copies the
     * unspent outputs whose slot is between {@code fromSlot} and {@code toSlot}, one transaction
     * per {@code chunkSlots} slots so a mainnet-sized fill never holds one long transaction.
     * Safe to repeat and to resume.
     *
     * <p>Run it with the table already enabled, so the commit listener covers new blocks, then
     * call {@link #reconcileSpentSince} with the tip slot from before the fill started: a chunk
     * can read an output as unspent just before the indexer commits its spend.
     *
     * @return the rows copied
     */
    public long backfill(long fromSlot, long toSlot, long chunkSlots) {
        if (chunkSlots <= 0) {
            throw new IllegalArgumentException("chunkSlots must be positive");
        }
        long copied = 0;
        for (long start = fromSlot; start <= toSlot; start += chunkSlots) {
            final long first = start;
            final long last = Math.min(start + chunkSlots - 1, toSlot);
            Integer rows = chunkTransaction.execute(status -> dsl.insertInto(ADDRESS_UTXO_UNSPENT, unspentColumns())
                    .select(select(addressUtxoColumns())
                            .from(ADDRESS_UTXO)
                            .where(ADDRESS_UTXO.SLOT.between(first, last))
                            .and(notSpent())
                            .and(notCopied()))
                    .execute());
            copied += rows == null ? 0 : rows;
            log.info("address_utxo_unspent backfill: slots {}..{} copied {} (total {})", first, last, rows, copied);
        }
        return copied;
    }

    /**
     * Deletes every row spent at or after {@code slot}. The step after a {@link #backfill}.
     *
     * @return the rows deleted
     */
    @Transactional
    public int reconcileSpentSince(long slot) {
        return deleteSpentBetween(slot, null);
    }

    /** Rows currently in the table. */
    public long count() {
        Record r = dsl.selectCount().from(ADDRESS_UTXO_UNSPENT).fetchOne();
        return r == null ? 0 : r.get(0, Long.class);
    }
}
