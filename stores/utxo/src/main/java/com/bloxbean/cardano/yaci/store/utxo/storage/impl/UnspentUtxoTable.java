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
import org.jooq.SQLDialect;
import org.jooq.SelectFieldOrAsterisk;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.*;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.row;
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
 * <p>The build runs in the background, so the sync does not wait for it (it takes 10 to 15 minutes on preprod
 * and about 2 hours on mainnet). It runs in one repeatable-read transaction, so it copies a consistent view as of
 * the cursor at its start. While the sync goes on, background passes then add the outputs created after that
 * point and delete the ones spent after it. When the table is close to the tip, the next commit runs the last
 * catch-up in its own transaction and switches the table on: from the following block on, every block writes it.
 * Rollbacks during the catch-up are applied to the table too. An interrupted build leaves the table not current,
 * and it runs again. It finds the unspent keys one output_index at a time, in tx_hash slices:
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
    /** Built, but not caught up with the store yet. */
    public static final String STATUS_BUILT = "BUILT";

    /** Within this many slots of the commit, the last catch-up runs in the commit and the table is switched on. */
    static final long SWITCH_ON_LAG_SLOTS = 600;

    enum Phase {
        /** Not checked yet */
        UNKNOWN,
        /** Not written and not read */
        OFF,
        /** Being built in the background */
        BUILDING,
        /** Built; rollbacks are applied to it, background passes catch up with the sync, not written by blocks yet */
        CATCHING_UP,
        /** Written by every block, and read */
        ACTIVE
    }

    /** Lower bounds of the tx_hash slices of one output_index; the last slice has no upper bound. */
    private static final String[] SLICE_BOUNDS = {"", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
    static final int COPY_BATCH_SIZE = 1000;
    private static final long PROGRESS_LOG_INTERVAL_MS = 10_000;

    private final UtxoStoreProperties properties;
    private final DSLContext dsl;
    private final TransactionTemplate buildTransactionTemplate;
    private final StoreStateService storeStateService;
    private final CursorService cursorService;

    private volatile Phase phase = Phase.UNKNOWN;
    //CATCHING_UP: the table is right for every block up to this slot. Changed only while holding tableLock.
    private volatile long caughtUpToSlot = -1;
    //BUILDING: the cursor slot of the build's snapshot, once known; and whether a rollback went below it
    private volatile Long buildSnapshotSlot;
    private volatile boolean buildInvalidated;
    //Serializes the background catch-up passes with rollbacks and the switch-on, which touch the same rows
    private final ReentrantLock tableLock = new ReentrantLock();
    private final java.util.concurrent.atomic.AtomicBoolean catchUpRunning = new java.util.concurrent.atomic.AtomicBoolean();
    private final TransactionTemplate catchUpTransactionTemplate;
    //Runs the background build. Tests replace it to run the build inline or at a chosen moment.
    java.util.concurrent.Executor buildExecutor = work -> Thread.ofVirtual().name("address-utxo-unspent-build").start(work);

    public UnspentUtxoTable(UtxoStoreProperties properties, DSLContext dsl, PlatformTransactionManager transactionManager,
                            StoreStateService storeStateService, CursorService cursorService) {
        this.properties = properties;
        this.dsl = dsl;
        this.buildTransactionTemplate = new TransactionTemplate(transactionManager);
        this.buildTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        //One consistent view of address_utxo, tx_input and the cursor for the whole build
        this.buildTransactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.catchUpTransactionTemplate = new TransactionTemplate(transactionManager);
        this.catchUpTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.storeStateService = storeStateService;
        this.cursorService = cursorService;
    }

    /** Whether the table is written: outputs inserted, spends deleted, rollbacks applied. */
    public boolean isActive() {
        if (properties == null || !properties.isUnspentTableEnabled())
            return false;

        if (phase == Phase.UNKNOWN) {
            synchronized (this) {
                if (phase == Phase.UNKNOWN)
                    phase = checkCurrent() ? Phase.ACTIVE : Phase.OFF;
            }
        }
        return phase == Phase.ACTIVE;
    }

    /** Whether a rollback must be applied to the table: it is active, or built and catching up. */
    public boolean isKeptOnRollback() {
        return isActive() || phase == Phase.CATCHING_UP;
    }

    Phase phase() {
        return phase;
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

        //The genesis block's cursor (slot -1) is written before the first block: still nothing synced
        if (cursor == null || cursor.getSlot() == null || cursor.getSlot() < 0) {
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

        startBuild("the sync starts from " + event.getFrom());
    }

    /**
     * Starts building the table in the background, unless it is active or already being built. The sync does not
     * wait: queries use address_utxo and tx_input until the table is switched on.
     */
    public void startBuild(String reason) {
        if (properties == null || !properties.isUnspentTableEnabled() || storeStateService == null)
            return;

        synchronized (this) {
            if (phase == Phase.ACTIVE || phase == Phase.BUILDING || phase == Phase.CATCHING_UP)
                return;
            phase = Phase.BUILDING;
            buildInvalidated = false;
            buildSnapshotSlot = null;
        }

        log.info("Building address_utxo_unspent in the background ({}). The sync goes on; queries use address_utxo and "
                + "tx_input until the table is ready (about 10 minutes on preprod, 2 hours on mainnet). Progress is "
                + "logged as it goes ...", reason);
        buildExecutor.execute(this::runBuild);
    }

    private void runBuild() {
        long start = System.currentTimeMillis();
        try {
            long[] copied = new long[1];
            long snapshotSlot = build(copied);

            boolean again;
            synchronized (this) {
                again = buildInvalidated;
                if (again) {
                    phase = Phase.OFF;
                } else {
                    caughtUpToSlot = snapshotSlot;
                    phase = Phase.CATCHING_UP;
                }
            }
            if (again) {
                log.warn("address_utxo_unspent: a rollback went below the build's starting point while it ran. Building again.");
                startBuild("a rollback during the build");
                return;
            }

            log.info("address_utxo_unspent built: {} unspent outputs as of slot {} in {} s. Catching up with the blocks "
                    + "synced meanwhile; it is switched on at a commit close to the tip.",
                    copied[0], snapshotSlot, (System.currentTimeMillis() - start) / 1000);
            catchUpInBackground();
        } catch (Exception e) {
            synchronized (this) {
                if (phase == Phase.BUILDING || phase == Phase.CATCHING_UP)
                    phase = Phase.OFF;
            }
            log.error("address_utxo_unspent build failed. Queries keep using address_utxo and tx_input; it is built "
                    + "again when the sync next starts", e);
        }
    }

    /**
     * Brings the table close to the store's cursor while the sync goes on, so the catch-up left for the switch-on
     * commit is small.
     */
    private void catchUpInBackground() {
        if (!catchUpRunning.compareAndSet(false, true))
            return;
        try {
            catchUpPasses();
        } catch (Exception e) {
            log.error("address_utxo_unspent catch-up failed; it is tried again at the next commit", e);
        } finally {
            catchUpRunning.set(false);
        }
    }

    private void catchUpPasses() {
        while (phase == Phase.CATCHING_UP) {
            Long cursorSlot = cursorService != null ? cursorService.getCursor().map(Cursor::getSlot).orElse(null) : null;
            if (cursorSlot == null || cursorSlot - caughtUpToSlot <= SWITCH_ON_LAG_SLOTS)
                return; //close enough: the next commit switches it on

            tableLock.lock();
            try {
                if (phase != Phase.CATCHING_UP)
                    return;
                long from = caughtUpToSlot;
                int[] changed = catchUpTransactionTemplate.execute(status -> catchUp(from));
                //The pass saw everything committed up to the cursor read before it, and maybe more. Rows past it are
                //looked at again by the next pass; that is harmless, catchUp is idempotent.
                caughtUpToSlot = Math.max(from, cursorSlot);
                log.info("address_utxo_unspent catch-up: slots {} to {}, {} outputs added, {} spent outputs deleted",
                        from, cursorSlot, changed != null ? changed[0] : 0, changed != null ? changed[1] : 0);
            } finally {
                tableLock.unlock();
            }
        }
    }

    /**
     * Adds the unspent outputs created after {@code fromSlot} and deletes the outputs spent after it. Safe to run
     * again over the same slots.
     *
     * @return the outputs added and the spent outputs deleted
     */
    int[] catchUp(long fromSlot) {
        int added = dsl.insertInto(ADDRESS_UTXO_UNSPENT, ADDRESS_UTXO_UNSPENT.fields())
                .select(select(addressUtxoColumns())
                        .from(ADDRESS_UTXO)
                        .where(ADDRESS_UTXO.SLOT.gt(fromSlot))
                        .and(notExists(selectOne().from(TX_INPUT)
                                .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                                .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))))
                        .and(notExists(selectOne().from(ADDRESS_UTXO_UNSPENT)
                                .where(ADDRESS_UTXO_UNSPENT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                                .and(ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)))))
                .execute();

        int deleted = dsl.deleteFrom(ADDRESS_UTXO_UNSPENT)
                .where(row(ADDRESS_UTXO_UNSPENT.TX_HASH, ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX)
                        .in(select(TX_INPUT.TX_HASH, TX_INPUT.OUTPUT_INDEX)
                                .from(TX_INPUT)
                                .where(TX_INPUT.SPENT_AT_SLOT.gt(fromSlot))))
                .execute();
        return new int[]{added, deleted};
    }

    /**
     * Called at every commit while the table is not active. When it is built and close to the commit, runs the last
     * catch-up in the commit's transaction (so it sees the blocks being committed), records the table as current
     * and switches it on: from the next block on, blocks write it.
     *
     * @return true if the table was switched on
     */
    boolean switchOnAtCommit(EventMetadata metadata) {
        if (phase != Phase.CATCHING_UP || metadata == null)
            return false;
        if (metadata.getSlot() - caughtUpToSlot > SWITCH_ON_LAG_SLOTS) {
            //The sync pulled ahead again: let the background passes close the distance
            if (!catchUpRunning.get())
                buildExecutor.execute(this::catchUpInBackground);
            return false;
        }
        if (!tableLock.tryLock())
            return false; //a background pass is running; try again at the next commit

        try {
            if (phase != Phase.CATCHING_UP)
                return false;
            long from = caughtUpToSlot;
            int[] changed = catchUp(from);
            keptTo(metadata);
            phase = Phase.ACTIVE;

            //If the commit fails, the catch-up and the state are rolled back with it: go back to catching up
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            synchronized (UnspentUtxoTable.this) {
                                if (phase == Phase.ACTIVE)
                                    phase = Phase.CATCHING_UP;
                            }
                        }
                    }
                });
            }
            log.info("address_utxo_unspent is current at block {} (last catch-up: {} outputs added, {} spent outputs "
                    + "deleted). It is kept current and used for queries from here on.",
                    metadata.getBlock(), changed[0], changed[1]);
            return true;
        } finally {
            tableLock.unlock();
        }
    }

    /**
     * Holds the table lock until the current transaction ends, so a background catch-up pass cannot interleave with
     * a rollback that is not committed yet. Without a transaction, runs the work under the lock.
     */
    void runLockedUntilTransactionEnds(Runnable work) {
        if (tableLock.isHeldByCurrentThread() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            tableLock.lock();
            try {
                work.run();
            } finally {
                tableLock.unlock();
            }
            return;
        }

        tableLock.lock();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                tableLock.unlock();
            }
        });
        work.run();
    }

    /**
     * Fills the table with every output in address_utxo that has no tx_input row, and records it as current
     * at the store's cursor, in one transaction.
     *
     * @param copied receives the number of rows copied
     * @return the cursor slot the build is consistent with (-1 if there is no cursor)
     */
    long build(long[] copied) {
        Long result = buildTransactionTemplate.execute(status -> {
            //A rebuild replaces a whole table: a delete would leave every old row behind as a dead tuple. TRUNCATE is
            //transactional only on PostgreSQL (H2 and MySQL commit at it), so the others delete.
            if (dsl.dialect().family() == SQLDialect.POSTGRES)
                dsl.truncate(ADDRESS_UTXO_UNSPENT).execute();
            else
                dsl.deleteFrom(ADDRESS_UTXO_UNSPENT).execute();

            Optional<Cursor> cursor = cursorService != null ? cursorService.getCursor() : Optional.empty();
            long snapshotSlot = cursor.map(Cursor::getSlot).orElse(-1L);
            buildSnapshotSlot = snapshotSlot;
            //Not current yet: blocks after the snapshot still have to be caught up
            storeStateService.saveState(StoreState.builder()
                    .stateId(STATE_ID)
                    .status(STATUS_BUILT)
                    .slot(cursor.map(Cursor::getSlot).orElse(null))
                    .block(cursor.map(Cursor::getBlock).orElse(null))
                    .blockHash(cursor.map(Cursor::getBlockHash).orElse(null))
                    .build());

            Integer maxOutputIndex = dsl.select(max(ADDRESS_UTXO.OUTPUT_INDEX)).from(ADDRESS_UTXO).fetchOne(0, Integer.class);
            if (maxOutputIndex == null)
                return snapshotSlot;

            long rows = 0;
            long start = System.currentTimeMillis();
            long lastLog = start;
            for (int outputIndex = 0; outputIndex <= maxOutputIndex; outputIndex++) {
                for (int slice = 0; slice < SLICE_BOUNDS.length; slice++) {
                    rows += copy(outputIndex, unspentKeys(outputIndex, slice));

                    //Per slice, not per output_index: most outputs have index 0, which alone takes about half the build
                    long now = System.currentTimeMillis();
                    if (now - lastLog >= PROGRESS_LOG_INTERVAL_MS) {
                        log.info("address_utxo_unspent build: output_index {} of {}, tx_hash slice {} of {}, {} rows copied, {} s elapsed",
                                outputIndex, maxOutputIndex, slice + 1, SLICE_BOUNDS.length, rows, (now - start) / 1000);
                        lastLog = now;
                    }
                }
            }
            copied[0] = rows;
            return snapshotSlot;
        });
        return result != null ? result : -1;
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

    /** The table's state row if it is current (the last block the table holds), else null. */
    StoreState currentState() {
        if (storeStateService == null)
            return null;

        return storeStateService.getState(STATE_ID)
                .filter(state -> STATUS_CURRENT.equals(state.getStatus()))
                .orElse(null);
    }

    /**
     * Checks that a batch starts right after the last block the table holds, or at a block it holds already
     * (a block processed again). That should always be so, because every commit moves the state with it.
     * <p>
     * If blocks were skipped (a gap), the table is missing their outputs and spends and cannot be trusted. Then
     * it is marked stale and switched off: queries go back to address_utxo and tx_input, the sync keeps running,
     * and the table is built again the next time the sync starts (restart or reconnect).
     *
     * @return true if there is a gap and the table was switched off
     */
    boolean stopIfGap(StoreState kept, Long firstBlock) {
        if (kept == null || kept.getBlock() == null || firstBlock == null || firstBlock <= kept.getBlock() + 1)
            return false;

        log.error("address_utxo_unspent missed {} blocks: it holds blocks up to {}, but this batch starts at block {}. "
                        + "It is switched off and marked stale: queries use address_utxo and tx_input, the sync continues, "
                        + "and the table is built again in the background.",
                firstBlock - kept.getBlock() - 1, kept.getBlock(), firstBlock);
        storeStateService.saveState(kept.toBuilder().status(STATUS_STALE).build());
        phase = Phase.OFF;
        return true;
    }

    /**
     * A rollback to {@code slot}. If the table was rolled back with the store it is current there. If not
     * (the table is off or not current) and its state is after the point, it still holds rows of blocks
     * that are gone, so it is marked stale and built again.
     */
    void rolledBackTo(long slot, boolean tableRolledBack) {
        if (storeStateService == null)
            return;

        if (tableRolledBack && phase == Phase.CATCHING_UP) {
            //Not current yet; the catch-up continues from the rollback point
            if (slot < caughtUpToSlot)
                caughtUpToSlot = slot;
            return;
        }

        if (phase == Phase.BUILDING) {
            Long snapshot = buildSnapshotSlot;
            if (snapshot == null || slot < snapshot)
                buildInvalidated = true;
        }

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
