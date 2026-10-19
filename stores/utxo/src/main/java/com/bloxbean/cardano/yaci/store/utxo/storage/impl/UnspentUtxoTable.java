package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.events.internal.PreBlockSyncEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectFieldOrAsterisk;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.*;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;

/**
 * Whether address_utxo_unspent is written and read, and its one-off build when
 * {@code store.utxo.unspent-table-backfill-at-tip} is on.
 *
 * <ul>
 *   <li>Without backfill-at-tip the table is written from the first block, so it is active whenever
 *   {@code store.utxo.unspent-table-enabled} is on.</li>
 *   <li>With it, nothing is written during the initial sync. On the first {@link PreBlockSyncEvent}
 *   (the store reaches the tip, or starts near it) the table is built from address_utxo minus tx_input
 *   and becomes active, and {@link UtxoStorageImpl} keeps it current from that block on.</li>
 * </ul>
 *
 * <p>The build runs in one transaction, so the table is either empty or complete: a non-empty table
 * means it was built (or written from the first block), and an interrupted build leaves it empty
 * and runs again at the next PreBlockSyncEvent. Until the table is active, the readers use
 * address_utxo and tx_input.
 *
 * <p>The build finds the unspent keys one output_index at a time, in tx_hash slices: both tables have
 * the primary key (output_index, tx_hash), so each slice is an anti-join of two ordered key ranges that
 * reads no row data. Only the outputs found unspent are copied with their columns. The admin CLI's
 * backfill-unspent-utxos runs the same steps.
 */
@Slf4j
public class UnspentUtxoTable {
    /** Lower bounds of the tx_hash slices of one output_index; the last slice has no upper bound. */
    private static final String[] SLICE_BOUNDS = {"", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
    static final int COPY_BATCH_SIZE = 1000;
    private static final long PROGRESS_LOG_INTERVAL_MS = 10_000;

    private final UtxoStoreProperties properties;
    private final DSLContext dsl;
    private final TransactionTemplate buildTransactionTemplate;

    //With backfill-at-tip: null until checked, then whether the table was built
    private volatile Boolean built;

    public UnspentUtxoTable(UtxoStoreProperties properties, DSLContext dsl, PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.dsl = dsl;
        this.buildTransactionTemplate = new TransactionTemplate(transactionManager);
        this.buildTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Whether the table is written: outputs inserted, spends deleted, rollbacks applied. */
    public boolean isActive() {
        if (properties == null || !properties.isUnspentTableEnabled())
            return false;
        if (!properties.isUnspentTableBackfillAtTip())
            return true;

        if (built == null)
            built = dsl.fetchExists(ADDRESS_UTXO_UNSPENT);
        return built;
    }

    /** Whether the unspent-output queries can be served from the table. */
    public boolean isReadable() {
        return properties != null && properties.isUnspentTableReadEnabled() && isActive();
    }

    @EventListener
    public void handlePreBlockSync(PreBlockSyncEvent event) {
        if (properties == null || !properties.isUnspentTableEnabled() || !properties.isUnspentTableBackfillAtTip() || isActive())
            return;

        log.info("Building address_utxo_unspent at the tip before the sync continues from {} ...", event.getFrom());
        long start = System.currentTimeMillis();
        try {
            long copied = build();
            built = true;
            log.info("address_utxo_unspent built: {} unspent outputs in {} s. It is kept current from here on.",
                    copied, (System.currentTimeMillis() - start) / 1000);
        } catch (Exception e) {
            //The sync continues without the table; the build runs again at the next PreBlockSyncEvent
            log.error("address_utxo_unspent build failed, it will be retried when the sync restarts", e);
        }
    }

    /**
     * Fills the table with every output in address_utxo that has no tx_input row, in one transaction.
     *
     * @return the rows copied
     */
    long build() {
        Long result = buildTransactionTemplate.execute(status -> {
            dsl.deleteFrom(ADDRESS_UTXO_UNSPENT).execute();

            Integer maxOutputIndex = dsl.select(max(ADDRESS_UTXO.OUTPUT_INDEX)).from(ADDRESS_UTXO).fetchOne(0, Integer.class);
            if (maxOutputIndex == null)
                return 0L;

            long copied = 0;
            long lastLog = System.currentTimeMillis();
            for (int outputIndex = 0; outputIndex <= maxOutputIndex; outputIndex++) {
                for (int slice = 0; slice < SLICE_BOUNDS.length; slice++) {
                    copied += copy(outputIndex, unspentKeys(outputIndex, slice));
                }
                if (System.currentTimeMillis() - lastLog >= PROGRESS_LOG_INTERVAL_MS) {
                    log.info("address_utxo_unspent build: output_index {} of {} done, {} rows copied", outputIndex, maxOutputIndex, copied);
                    lastLog = System.currentTimeMillis();
                }
            }
            return copied;
        });
        return result != null ? result : 0;
    }

    private List<String> unspentKeys(int outputIndex, int slice) {
        String lower = SLICE_BOUNDS[slice];
        String upper = slice < SLICE_BOUNDS.length - 1 ? SLICE_BOUNDS[slice + 1] : null;
        Condition outerUpper = upper != null ? ADDRESS_UTXO.TX_HASH.lt(upper) : noCondition();
        //The inner range repeats the outer one so the anti-join reads only this slice of tx_input too
        Condition innerUpper = upper != null ? TX_INPUT.TX_HASH.lt(upper) : noCondition();

        return dsl.select(ADDRESS_UTXO.TX_HASH)
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OUTPUT_INDEX.eq(outputIndex))
                .and(ADDRESS_UTXO.TX_HASH.ge(lower))
                .and(outerUpper)
                .and(notExists(selectOne().from(TX_INPUT)
                        .where(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                        .and(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                        .and(TX_INPUT.TX_HASH.ge(lower))
                        .and(innerUpper)))
                .fetch(ADDRESS_UTXO.TX_HASH);
    }

    private long copy(int outputIndex, List<String> txHashes) {
        long copied = 0;
        for (int from = 0; from < txHashes.size(); from += COPY_BATCH_SIZE) {
            List<String> batch = txHashes.subList(from, Math.min(from + COPY_BATCH_SIZE, txHashes.size()));
            copied += dsl.insertInto(ADDRESS_UTXO_UNSPENT, ADDRESS_UTXO_UNSPENT.fields())
                    .select(select(addressUtxoColumns())
                            .from(ADDRESS_UTXO)
                            .where(ADDRESS_UTXO.OUTPUT_INDEX.eq(outputIndex))
                            .and(ADDRESS_UTXO.TX_HASH.in(batch)))
                    .execute();
        }
        return copied;
    }

    /** The address_utxo columns in the order of address_utxo_unspent's, for insert-selects. */
    static List<SelectFieldOrAsterisk> addressUtxoColumns() {
        return Arrays.stream(ADDRESS_UTXO_UNSPENT.fields())
                .map(field -> (SelectFieldOrAsterisk) ADDRESS_UTXO.field(field.getName()))
                .toList();
    }
}
