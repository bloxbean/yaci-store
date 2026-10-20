package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.domain.StoreState;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.service.StoreStateService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
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
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.*;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;

/**
 * Whether address_utxo_unspent is written and read, and its one-off build.
 *
 * <p>The table is only correct if every block since it was built went through {@link UtxoStorageImpl}
 * with the table on. Its store_state row ({@link #STATE_ID}) records the block it is kept to: the build
 * sets it, every commit moves it in the same transaction as the commit's deletes, and a rollback moves it
 * back. At startup the table is active only if that block is not behind the store's cursor. A cursor
 * ahead of the state means blocks went through without the table (the flag was off, or an older version
 * ran), so the table is left alone, the readers use address_utxo and tx_input, and the table is built
 * again on the next {@link PreBlockSyncEvent} (the store reaches the tip, or starts near it).
 *
 * <ul>
 *   <li>Without backfill-at-tip, a store that has not synced a block keeps the table from the first block.</li>
 *   <li>With it, nothing is written during the initial sync, and the table is built at the tip.</li>
 * </ul>
 *
 * <p>The build runs in one transaction with its state, so an interrupted build leaves the table not
 * current and it runs again. It finds the unspent keys one output_index at a time, in tx_hash slices:
 * both tables have the primary key (output_index, tx_hash), so each slice is an anti-join of two ordered
 * key ranges that reads no row data. Only the outputs found unspent are copied with their columns. The
 * admin CLI's backfill-unspent-utxos runs the same steps.
 */
@Slf4j
public class UnspentUtxoTable {
    /** The table's row in store_state. The admin CLI (UnspentUtxoBackfillService in dbutils) uses the same id. */
    public static final String STATE_ID = "utxo.unspent_table";
    /** The table equals address_utxo minus tx_input at the state's block. */
    public static final String STATUS_CURRENT = "CURRENT";
    /** Blocks were rolled back while the table was off, so it holds rows of blocks that are gone. */
    public static final String STATUS_STALE = "STALE";

    /** Lower bounds of the tx_hash slices of one output_index; the last slice has no upper bound. */
    private static final String[] SLICE_BOUNDS = {"", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
    static final int COPY_BATCH_SIZE = 1000;
    private static final long PROGRESS_LOG_INTERVAL_MS = 10_000;

    private final UtxoStoreProperties properties;
    private final DSLContext dsl;
    private final TransactionTemplate buildTransactionTemplate;
    private final StoreStateService storeStateService;
    private final CursorService cursorService;

    //null until checked at the first use, then whether the table is current
    private volatile Boolean current;

    public UnspentUtxoTable(UtxoStoreProperties properties, DSLContext dsl, PlatformTransactionManager transactionManager,
                            StoreStateService storeStateService, CursorService cursorService) {
        this.properties = properties;
        this.dsl = dsl;
        this.buildTransactionTemplate = new TransactionTemplate(transactionManager);
        this.buildTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.storeStateService = storeStateService;
        this.cursorService = cursorService;
    }

    /** Whether the table is written: outputs inserted, spends deleted, rollbacks applied. */
    public boolean isActive() {
        if (properties == null || !properties.isUnspentTableEnabled())
            return false;

        if (current == null)
            current = checkCurrent();
        return current;
    }

    private boolean checkCurrent() {
        if (storeStateService == null) {
            log.warn("address_utxo_unspent is not used: there is no store_state service to record how far it is kept");
            return false;
        }

        StoreState state = storeStateService.getState(STATE_ID)
                .filter(s -> STATUS_CURRENT.equals(s.getStatus()))
                .orElse(null);
        Cursor cursor = cursorService != null ? cursorService.getCursor().orElse(null) : null;

        if (cursor == null) {
            //Nothing synced yet: from genesis the table is kept from the first block, if nothing is left in it
            return state != null || (!properties.isUnspentTableBackfillAtTip() && !dsl.fetchExists(ADDRESS_UTXO_UNSPENT));
        }

        if (state != null && state.getSlot() != null && state.getSlot() >= cursor.getSlot())
            return true;

        log.warn("address_utxo_unspent is not current: it is kept to slot {}, the store is at slot {}. "
                        + "Queries use address_utxo and tx_input until it is built when the sync reaches the tip.",
                state != null ? state.getSlot() : null, cursor.getSlot());
        return false;
    }

    /** Whether the unspent-output queries can be served from the table. */
    public boolean isReadable() {
        return properties != null && properties.isUnspentTableReadEnabled() && isActive();
    }

    @EventListener
    public void handlePreBlockSync(PreBlockSyncEvent event) {
        if (properties == null || !properties.isUnspentTableEnabled() || storeStateService == null || isActive())
            return;

        log.info("Building address_utxo_unspent at the tip before the sync continues from {} ...", event.getFrom());
        long start = System.currentTimeMillis();
        try {
            long copied = build();
            current = true;
            log.info("address_utxo_unspent built: {} unspent outputs in {} s. It is kept current from here on.",
                    copied, (System.currentTimeMillis() - start) / 1000);
        } catch (Exception e) {
            //The sync continues without the table; the build runs again at the next PreBlockSyncEvent
            log.error("address_utxo_unspent build failed, it will be retried when the sync restarts", e);
        }
    }

    /**
     * Fills the table with every output in address_utxo that has no tx_input row, and records it as current
     * at the store's cursor, in one transaction.
     *
     * @return the rows copied
     */
    long build() {
        Long result = buildTransactionTemplate.execute(status -> {
            dsl.deleteFrom(ADDRESS_UTXO_UNSPENT).execute();

            Optional<Cursor> cursor = cursorService != null ? cursorService.getCursor() : Optional.empty();
            storeStateService.saveState(StoreState.builder()
                    .stateId(STATE_ID)
                    .status(STATUS_CURRENT)
                    .slot(cursor.map(Cursor::getSlot).orElse(null))
                    .block(cursor.map(Cursor::getBlock).orElse(null))
                    .blockHash(cursor.map(Cursor::getBlockHash).orElse(null))
                    .build());

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

    /**
     * Records that the table is current at the end of a batch. Called in the transaction that applies the
     * batch's spends to the table.
     */
    void keptTo(EventMetadata metadata) {
        storeStateService.saveState(StoreState.builder()
                .stateId(STATE_ID)
                .status(STATUS_CURRENT)
                .slot(metadata.getSlot())
                .block(metadata.getBlock())
                .blockHash(metadata.getBlockHash())
                .build());
    }

    /**
     * A rollback to {@code slot}. If the table was rolled back with the store it is current there. If not
     * (the table is off or not current) and its state is after the point, it still holds rows of blocks
     * that are gone, so it is marked stale and built again.
     */
    void rolledBackTo(long slot, boolean tableRolledBack) {
        if (storeStateService == null)
            return;

        if (tableRolledBack) {
            storeStateService.saveState(StoreState.builder()
                    .stateId(STATE_ID)
                    .status(STATUS_CURRENT)
                    .slot(slot)
                    .build());
            return;
        }

        storeStateService.getState(STATE_ID)
                .filter(state -> STATUS_CURRENT.equals(state.getStatus()))
                .filter(state -> state.getSlot() == null || state.getSlot() > slot)
                .ifPresent(state -> storeStateService.saveState(state.toBuilder().status(STATUS_STALE).build()));
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
