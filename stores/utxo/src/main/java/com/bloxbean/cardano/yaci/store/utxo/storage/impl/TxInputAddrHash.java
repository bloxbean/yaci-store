package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.domain.StoreState;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.service.StoreStateService;
import com.bloxbean.cardano.yaci.store.events.internal.PreBlockSyncEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

/**
 * tx_input.addr_hash: a 64-bit hash of the owner_addr of the output each tx_input row spends. With it, and an
 * index on the same hash of address_utxo.owner_addr, the transactions of an address are read newest first from
 * two indexes ({@link UtxoStorageReaderImpl#findTransactionsByAddress}) instead of from its whole history.
 * PostgreSQL only: the hash is {@code hashtextextended(owner_addr, 0)}, the hash PostgreSQL's hash partitioning
 * uses, so it is computed in SQL and stays the same across upgrades. On H2 and MySQL nothing here is active.
 *
 * <p>Its store_state row ({@link #STATE_ID}) records how far tx_input has the hash:
 * <ul>
 *   <li>No row: nothing is written, as in the initial sync, which stays as fast as before.</li>
 *   <li>The first {@link PreBlockSyncEvent} (the store reaches the tip, or starts near it) creates the row as
 *   {@link #STATUS_FILLING}. From then on every new tx_input row gets the hash when it is inserted, and a background
 *   thread fills the older rows in slot chunks while the sync continues. The row's slot is how far the fill got:
 *   every row up to it has the hash. Then the thread builds the two indexes and sets {@link #STATUS_CURRENT}.</li>
 *   <li>{@link #STATUS_CURRENT}: queries use the hash, and each commit moves the slot to its last block.</li>
 * </ul>
 * A store that synced blocks without the hash (the flag was off for a while) has a slot behind its cursor at
 * startup: it goes back to filling from that slot. A rollback moves the slot back to the rollback point.
 */
@Slf4j
public class TxInputAddrHash {
    public static final String STATE_ID = "utxo.tx_input_addr_hash";
    public static final String STATUS_FILLING = "FILLING";
    public static final String STATUS_CURRENT = "CURRENT";

    static final String TX_INPUT_INDEX = "idx_tx_input_addr_hash_slot";
    static final String ADDRESS_UTXO_INDEX = "idx_address_utxo_addr_hash_slot";

    /** Rows per fill chunk: each chunk is one UPDATE that reads address_utxo once, then tx_input is vacuumed. */
    private static final long DEFAULT_CHUNK_ROWS = 50_000_000;
    /**
     * The last stretch before the tip is filled in its own small chunk, so a chain rollback, which deletes the
     * newest rows, never waits for a long chunk. About half a day of mainnet.
     */
    private static final long RECENT_SLOTS = 50_000;
    /** How often a store that is not current re-reads store_state, for read-only instances that do not fill. */
    private static final long RECHECK_INTERVAL_MS = 60_000;

    private final UtxoStoreProperties properties;
    private final DSLContext dsl;
    private final TransactionTemplate transactionTemplate;
    private final StoreStateService storeStateService;
    private final CursorService cursorService;

    //null until checked at the first use
    private volatile Boolean live;
    private volatile boolean current;
    private volatile long lastCheck;
    private final AtomicBoolean filling = new AtomicBoolean(false);
    private final AtomicBoolean warned = new AtomicBoolean(false);

    //Package-private so tests can fill in several chunks
    long chunkRows = DEFAULT_CHUNK_ROWS;
    long recentSlots = RECENT_SLOTS;

    public TxInputAddrHash(UtxoStoreProperties properties, DSLContext dsl, PlatformTransactionManager transactionManager,
                           StoreStateService storeStateService, CursorService cursorService) {
        this.properties = properties;
        this.dsl = dsl;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.storeStateService = storeStateService;
        this.cursorService = cursorService;
    }

    /** {@code hashtextextended(value, 0)}: the address hash, of a column or of a bound address. */
    static Field<Long> hash(Field<String> address) {
        return DSL.field("hashtextextended({0}, 0)", Long.class, address);
    }

    private boolean enabled() {
        return properties != null && properties.isTxInputAddrHashEnabled() && storeStateService != null
                && dsl.dialect().family() == SQLDialect.POSTGRES;
    }

    /** Whether new tx_input rows get the hash: from the first time the store reached the tip with the flag on. */
    public boolean isLive() {
        if (!enabled())
            return false;

        if (live == null)
            init();
        return live;
    }

    /** Whether every tx_input row has the hash and the indexes exist, so the transactions query can use them. */
    public boolean isReadable() {
        if (!enabled())
            return false;

        if (!current && System.currentTimeMillis() - lastCheck > RECHECK_INTERVAL_MS) {
            lastCheck = System.currentTimeMillis();
            current = storeStateService.getState(STATE_ID)
                    .map(state -> STATUS_CURRENT.equals(state.getStatus()))
                    .orElse(false);
        }
        return current;
    }

    private synchronized void init() {
        if (live != null)
            return;

        StoreState state = storeStateService.getState(STATE_ID).orElse(null);
        if (state == null) {
            live = false;
            return;
        }

        //A current state behind the cursor: blocks were synced while the flag was off, so their rows have no hash
        Long cursorSlot = cursorService != null ? cursorService.getCursor().map(Cursor::getSlot).orElse(null) : null;
        if (STATUS_CURRENT.equals(state.getStatus()) && cursorSlot != null
                && (state.getSlot() == null || state.getSlot() < cursorSlot)) {
            log.warn("tx_input.addr_hash is set up to slot {}, but the store is at slot {}: blocks were synced without it. "
                    + "/addresses/{address}/transactions uses its full query until the rows after slot {} are filled, "
                    + "which starts when the sync reaches the tip.", state.getSlot(), cursorSlot, state.getSlot());
            storeStateService.saveState(state.toBuilder().status(STATUS_FILLING).build());
            state = state.toBuilder().status(STATUS_FILLING).build();
        }

        current = STATUS_CURRENT.equals(state.getStatus());
        lastCheck = System.currentTimeMillis();
        live = true;
    }

    @EventListener
    public void handlePreBlockSync(PreBlockSyncEvent event) {
        if (properties == null || !properties.isTxInputAddrHashEnabled())
            return;

        if (!enabled()) {
            if (warned.compareAndSet(false, true))
                log.warn("store.utxo.tx-input-addr-hash-enabled is ignored: it needs PostgreSQL and the store_state table, "
                        + "the store runs on {}", dsl.dialect().family());
            return;
        }

        isLive();
        if (current || !filling.compareAndSet(false, true))
            return;

        //From here on new rows get the hash, before any block of this sync is written
        if (!live) {
            storeStateService.saveState(StoreState.builder().stateId(STATE_ID).status(STATUS_FILLING).build());
            live = true;
        }

        Thread thread = new Thread(() -> {
            try {
                fill();
            } catch (Exception e) {
                log.error("tx_input.addr_hash fill failed. It continues from where it stopped when the sync restarts", e);
            } finally {
                filling.set(false);
            }
        }, "tx-input-addr-hash-fill");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Fills the hash of the tx_input rows up to the cursor slot at the start, in chunks of about {@link #chunkRows}
     * rows by spent_at_slot, then builds the indexes and marks the state current. Each chunk commits with the
     * state's slot moved to its end, so a restart continues after the last finished chunk.
     */
    void fill() {
        long start = System.currentTimeMillis();
        StoreState state = storeStateService.getState(STATE_ID).orElseThrow();
        long from = state.getSlot() != null ? state.getSlot() : -1;
        Long target = cursorService != null ? cursorService.getCursor().map(Cursor::getSlot).orElse(null) : null;
        if (target == null)
            target = dsl.select(DSL.max(TX_INPUT.SPENT_AT_SLOT)).from(TX_INPUT).fetchOne(0, Long.class);

        log.info("tx_input.addr_hash: filling the rows spent after slot {} up to slot {} in the background, "
                + "about 3 to 4 hours on mainnet. The sync continues; /addresses/{address}/transactions uses its full "
                + "query until this is done.", from, target);

        long filled = 0;
        long recentFrom = target != null ? target - recentSlots : Long.MAX_VALUE;
        while (target != null && from < target) {
            long chunkFrom = from;
            //Large chunks up to the recent stretch, then that stretch in one small chunk
            long to = from < recentFrom ? Math.min(chunkEnd(from), recentFrom) : target;
            boolean large = from < recentFrom;
            Integer rows = transactionTemplate.execute(status -> {
                //A large chunk is joined by hashing it and reading address_utxo once, never row by row
                if (large)
                    dsl.execute("set local enable_nestloop = off");
                int updated = fillRange(chunkFrom, to);
                storeStateService.saveState(StoreState.builder().stateId(STATE_ID).status(STATUS_FILLING).slot(to).build());
                return updated;
            });
            filled += rows != null ? rows : 0;
            //Frees the old row versions now, so the next chunk reuses their space instead of growing the table
            dsl.execute("vacuum " + TX_INPUT.getName());
            log.info("tx_input.addr_hash: filled up to slot {} of {}, {} rows so far, {} s elapsed",
                    to, target, filled, (System.currentTimeMillis() - start) / 1000);
            from = to;
        }

        createIndexes();

        Long now = cursorService != null ? cursorService.getCursor().map(Cursor::getSlot).orElse(target) : target;
        storeStateService.saveState(StoreState.builder().stateId(STATE_ID).status(STATUS_CURRENT).slot(now).build());
        current = true;
        log.info("tx_input.addr_hash is current: {} rows filled and indexes built in {} s. "
                + "/addresses/{address}/transactions now reads from it.", filled, (System.currentTimeMillis() - start) / 1000);
    }

    /** The slot about {@link #chunkRows} rows after {@code from}, or the last slot when fewer rows are left. */
    private long chunkEnd(long from) {
        Long end = dsl.select(TX_INPUT.SPENT_AT_SLOT).from(TX_INPUT)
                .where(TX_INPUT.SPENT_AT_SLOT.gt(from))
                .orderBy(TX_INPUT.SPENT_AT_SLOT)
                .offset(chunkRows)
                .limit(1)
                .fetchOne(0, Long.class);
        return end != null ? end : Long.MAX_VALUE;
    }

    /** Sets the hash of the rows spent in slots (from, to] that do not have it yet. */
    int fillRange(long from, long to) {
        var au = ADDRESS_UTXO.as("au");
        return dsl.update(TX_INPUT)
                .set(TX_INPUT.ADDR_HASH, hash(au.OWNER_ADDR))
                .from(au)
                .where(TX_INPUT.SPENT_AT_SLOT.gt(from))
                .and(TX_INPUT.SPENT_AT_SLOT.le(to))
                .and(TX_INPUT.ADDR_HASH.isNull())
                .and(au.TX_HASH.eq(TX_INPUT.TX_HASH))
                .and(au.OUTPUT_INDEX.eq(TX_INPUT.OUTPUT_INDEX))
                .execute();
    }

    /**
     * Builds the two indexes without blocking writes. A build that was interrupted leaves an invalid index,
     * which is dropped and built again.
     */
    private void createIndexes() {
        String txInput = TX_INPUT.getName();
        String addressUtxo = ADDRESS_UTXO.getName();
        createIndex(TX_INPUT_INDEX, "create index concurrently if not exists " + TX_INPUT_INDEX
                + " on " + txInput + " (addr_hash, spent_at_slot)");
        createIndex(ADDRESS_UTXO_INDEX, "create index concurrently if not exists " + ADDRESS_UTXO_INDEX
                + " on " + addressUtxo + " (hashtextextended(owner_addr, 0), slot)");
        //The planner estimates the expression's selectivity from statistics that only ANALYZE collects
        dsl.execute("analyze " + addressUtxo);
        dsl.execute("analyze " + txInput);
    }

    private void createIndex(String name, String sql) {
        Boolean invalid = dsl.fetchOptional("select not i.indisvalid from pg_index i join pg_class c on c.oid = i.indexrelid "
                        + "where c.relname = ? and pg_table_is_visible(c.oid)", name)
                .map(record -> record.get(0, Boolean.class))
                .orElse(null);
        if (Boolean.TRUE.equals(invalid))
            dsl.execute("drop index concurrently if exists " + name);

        long start = System.currentTimeMillis();
        log.info("tx_input.addr_hash: building index {} ...", name);
        dsl.execute(sql);
        log.info("tx_input.addr_hash: index {} built in {} s", name, (System.currentTimeMillis() - start) / 1000);
    }

    /**
     * Sets the hash of the batch's rows that were inserted without it: in a batch a block's spends can be saved
     * before the output they spend, which a later block or transaction of the batch writes. Runs in the commit's
     * transaction; once current, the state's slot moves to the batch's last block in the same transaction.
     *
     * @param fromSlot the first slot of the spends saved since the last commit, null if there were none
     * @param toSlot   the last slot of those spends, null if there were none
     * @param commitSlot the slot of the batch's last block
     */
    void committed(Long fromSlot, Long toSlot, long commitSlot) {
        if (!isLive())
            return;

        if (fromSlot != null && toSlot != null)
            fillRange(fromSlot - 1, toSlot);
        if (current)
            storeStateService.saveState(StoreState.builder().stateId(STATE_ID).status(STATUS_CURRENT).slot(commitSlot).build());
    }

    /** A rollback to {@code slot}: every row up to the point still has the hash, the rows after it are gone. */
    void rolledBackTo(long slot) {
        if (storeStateService == null || dsl.dialect().family() != SQLDialect.POSTGRES)
            return;

        storeStateService.getState(STATE_ID)
                .filter(state -> state.getSlot() != null && state.getSlot() > slot)
                .ifPresent(state -> storeStateService.saveState(state.toBuilder().slot(slot).build()));
    }
}
