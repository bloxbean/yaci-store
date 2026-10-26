package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.cache.Cache;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.domain.StoreState;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
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

import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

/**
 * tx_input.owner_addr: the owner_addr of the output each tx_input row spends. With it, and an index on
 * address_utxo by a hash of owner_addr, the transactions of an address are read newest first from two indexes
 * ({@link UtxoStorageReaderImpl#findTransactionsByAddress}) instead of from its whole history. PostgreSQL 16+ only,
 * behind {@code store.utxo.tx-input-owner-addr-enabled}; on H2, MySQL and older PostgreSQL nothing here is active.
 *
 * <p>The address is written with the row, from the first block: {@link UtxoStorageImpl} keeps the address of each
 * unspent output in a cache ({@link #cache()}, a column family of the process's RocksDB cache) and looks the spent
 * output up there, or in address_utxo when the cache does not have it. The cache never decides a result: an output's
 * address never changes. It is for the initial sync, where a block spends outputs of any age: a restart during it
 * keeps the cache, so the sync does not slow down while the cache refills. When the sync reaches the tip the cache is
 * cleared and not used again: a block's spends are read from address_utxo, one query per block. A restart after the
 * tip, or of a new store (a folder left by an earlier database), clears it too.
 *
 * <p>Its store_state row ({@link #STATE_ID}) records up to which slot every tx_input row has the address:
 * <ul>
 *   <li>A new store (no tx_input row yet) starts at its cursor, and each commit moves the slot to its last block. A store synced before the
 *   flag was on starts with no slot: the background fill below brings it up to date.</li>
 *   <li>At the first {@link PreBlockSyncEvent} (the store reaches the tip, or starts near it) a background thread fills
 *   the rows that are still missing the address, if any, and sets {@link #STATUS_CURRENT}. On a new store there is
 *   nothing to fill.</li>
 *   <li>{@link #STATUS_CURRENT} and both indexes built: queries use the address. The indexes are optional indexes
 *   (dbutils index.yml), applied after the sync with {@code admin-cli apply-indexes}; until they are valid the full
 *   query is used.</li>
 * </ul>
 * A store that saves tx_input rows without the address (the flag was turned off) puts the state back to filling from
 * before those rows; readers also check that the state is at or past the cursor, which covers a writer of a version
 * without this class. A rollback moves the slot back to the rollback point.
 */
@Slf4j
public class TxInputOwnerAddr {
    public static final String STATE_ID = "utxo.tx_input_owner_addr";
    public static final String STATUS_FILLING = "FILLING";
    public static final String STATUS_CURRENT = "CURRENT";
    /** The cache's column family in the RocksDB cache. */
    public static final String CACHE_NAME = "utxo_owner_addr";

    /** Defined in dbutils index.yml, which admin-cli apply-indexes applies. */
    static final String TX_INPUT_INDEX = "idx_tx_input_owner_addr_slot";
    static final String ADDRESS_UTXO_INDEX = "idx_address_utxo_addr_hash_slot";

    /** Rows per fill chunk: each chunk is one UPDATE that reads address_utxo once, then tx_input is vacuumed. */
    private static final long DEFAULT_CHUNK_ROWS = 50_000_000;
    /** The last stretch before the tip is filled in a small chunk, so a chain rollback never waits for a long one. */
    private static final long RECENT_SLOTS = 50_000;
    /** Memory for a fill chunk's hash join: the less fits, the more is written to temporary files. */
    private static final String FILL_WORK_MEM = "2GB";
    /** Memory for the VACUUM after each fill chunk. */
    private static final String VACUUM_MEM = "1GB";
    /** How often the state and the indexes are re-checked: by read-only instances, and for indexes applied later. */
    private static final long RECHECK_INTERVAL_MS = 60_000;

    private final UtxoStoreProperties properties;
    private final DSLContext dsl;
    private final TransactionTemplate transactionTemplate;
    private final StoreStateService storeStateService;
    private final CursorService cursorService;
    private final Supplier<Cache<UtxoKey, String>> cacheSupplier;

    private volatile Boolean postgres16;
    private volatile boolean initialized;
    private volatile boolean current;
    private volatile boolean readable;
    //Whether every row up to the state's slot had the address when this run started: then each commit moves it
    private volatile boolean keptToCommits;
    private volatile long lastCheck;
    private volatile Cache<UtxoKey, String> cache;
    //Set when the sync reaches the tip: from then on spends are read from address_utxo
    private volatile boolean cacheRetired;
    private final AtomicBoolean filling = new AtomicBoolean(false);
    private final AtomicBoolean warned = new AtomicBoolean(false);
    private final AtomicBoolean indexHintLogged = new AtomicBoolean(false);
    private final AtomicBoolean savedWithoutAddrChecked = new AtomicBoolean(false);

    //Package-private so tests can fill in several chunks
    long chunkRows = DEFAULT_CHUNK_ROWS;
    long recentSlots = RECENT_SLOTS;

    /**
     * @param cacheSupplier makes the cache of output addresses, called once and only if the address is written
     */
    public TxInputOwnerAddr(UtxoStoreProperties properties, DSLContext dsl, PlatformTransactionManager transactionManager,
                            StoreStateService storeStateService, CursorService cursorService,
                            Supplier<Cache<UtxoKey, String>> cacheSupplier) {
        this.properties = properties;
        this.dsl = dsl;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.storeStateService = storeStateService;
        this.cursorService = cursorService;
        this.cacheSupplier = cacheSupplier;
    }

    /** {@code hashtextextended(value, 0)}, the key of the address_utxo index: of a column or of a bound address. */
    static Field<Long> hash(Field<String> address) {
        return DSL.field("hashtextextended({0}, 0)", Long.class, address);
    }

    private boolean enabled() {
        return properties != null && properties.isTxInputOwnerAddrEnabled() && storeStateService != null
                && dsl.dialect().family() == SQLDialect.POSTGRES && postgres16();
    }

    private boolean postgres16() {
        if (postgres16 == null) {
            try {
                postgres16 = Integer.parseInt(String.valueOf(dsl.fetchValue("show server_version_num"))) >= 160000;
            } catch (Exception e) {
                log.warn("tx_input.owner_addr: could not read the PostgreSQL version, so it is not used", e);
                postgres16 = false;
            }
            if (!postgres16)
                log.warn("store.utxo.tx-input-owner-addr-enabled is ignored: it needs PostgreSQL 16 or later");
        }
        return postgres16;
    }

    /** Whether tx_input rows are written with the address. */
    public boolean isLive() {
        if (!enabled())
            return false;

        if (!initialized)
            init();
        return true;
    }

    /**
     * The addresses of unspent outputs, by output, until the sync reaches the tip; null from then on. Only while
     * {@link #isLive()}.
     */
    Cache<UtxoKey, String> cache() {
        if (cacheRetired)
            return null;
        return openCache();
    }

    private Cache<UtxoKey, String> openCache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null)
                    cache = cacheSupplier.get();
            }
        }
        return cache;
    }

    /** Whether every tx_input row has the address and the indexes exist, so the transactions query can use them. */
    public boolean isReadable() {
        if (!enabled())
            return false;

        long now = System.currentTimeMillis();
        if (now - lastCheck > RECHECK_INTERVAL_MS) {
            lastCheck = now;
            readable = checkCurrent() && checkIndexes();
        }
        return readable;
    }

    /** Re-checks the state and the indexes at the next {@link #isReadable()}. */
    void recheck() {
        lastCheck = 0;
    }

    /** CURRENT, and kept to the cursor or past it: the cursor is saved after the commit that moves the state. */
    private boolean checkCurrent() {
        StoreState state = storeStateService.getState(STATE_ID).orElse(null);
        if (state == null || !STATUS_CURRENT.equals(state.getStatus()) || state.getSlot() == null)
            return false;

        Long cursorSlot = cursorSlot();
        return cursorSlot == null || state.getSlot() >= cursorSlot;
    }

    /** Both indexes exist and are valid: without them the query would read the whole of both tables. */
    private boolean checkIndexes() {
        Integer valid = dsl.fetchOne("select count(*)::int from pg_index i join pg_class c on c.oid = i.indexrelid "
                + "where i.indisvalid and c.relname in (?, ?) and pg_table_is_visible(c.oid)", TX_INPUT_INDEX, ADDRESS_UTXO_INDEX)
                .get(0, Integer.class);
        boolean built = valid == 2;
        if (!built && indexHintLogged.compareAndSet(false, true))
            log.info("tx_input.owner_addr is current, but its indexes {} and {} are not built yet. Apply them with "
                    + "admin-cli apply-indexes; /addresses/{address}/transactions uses its full query until then.",
                    TX_INPUT_INDEX, ADDRESS_UTXO_INDEX);
        if (built)
            indexHintLogged.set(false);
        return built;
    }

    private Long cursorSlot() {
        return cursorService != null ? cursorService.getCursor().map(Cursor::getSlot).orElse(null) : null;
    }

    private synchronized void init() {
        if (initialized)
            return;

        StoreState state = storeStateService.getState(STATE_ID).orElse(null);
        boolean stateBefore = state != null;
        Long cursorSlot = cursorSlot();
        if (state == null) {
            //A store with no tx_input row yet has the address from its first spend, even if its cursor is already
            //set (at the genesis block); one with rows synced before gets them filled at the tip
            boolean newStore = !dsl.fetchExists(TX_INPUT);
            Long slot = newStore ? (cursorSlot != null ? cursorSlot : 0L) : null;
            state = StoreState.builder().stateId(STATE_ID).status(STATUS_FILLING).slot(slot).build();
            storeStateService.saveState(state);
            if (!newStore)
                log.info("tx_input.owner_addr: new tx_input rows get the address from now on. The rows synced so far are "
                        + "filled in the background when the sync reaches the tip.");
            keptToCommits = newStore;
        } else if (STATUS_CURRENT.equals(state.getStatus()) && cursorSlot != null
                && (state.getSlot() == null || state.getSlot() < cursorSlot)) {
            //Blocks were synced by a version without the address
            log.warn("tx_input.owner_addr is set up to slot {}, but the store is at slot {}: blocks were synced without it. "
                    + "/addresses/{address}/transactions uses its full query until the rows after slot {} are filled, "
                    + "which starts when the sync reaches the tip.", state.getSlot(), cursorSlot, state.getSlot());
            state = state.toBuilder().status(STATUS_FILLING).build();
            storeStateService.saveState(state);
            keptToCommits = false;
        } else {
            //Not behind the cursor: the last run kept it to its last commit (the cursor is saved after the commit)
            keptToCommits = state.getSlot() != null && (cursorSlot == null || state.getSlot() >= cursorSlot);
        }

        current = STATUS_CURRENT.equals(state.getStatus());
        //The last run's cache is kept only while the initial sync it was made for goes on
        if (!(stateBefore && keptToCommits && !current))
            clearCache("the store " + (current ? "has reached the tip before" : "starts its tx_input.owner_addr state"));
        initialized = true;
    }

    private void clearCache(String reason) {
        long start = System.currentTimeMillis();
        Cache<UtxoKey, String> cache = openCache();
        long entries = cache.size();
        cache.clear();
        if (entries > 0)
            log.info("tx_input.owner_addr: cleared the cache of about {} output addresses in {} ms, as {}",
                    entries, System.currentTimeMillis() - start, reason);
    }

    /**
     * A commit of a batch whose rows all have the address. If every row before the batch had it too (the state was
     * kept to the commits since this run started), the state moves to the batch's last slot. Runs in the commit's
     * transaction. The cursor is not compared here: a Byron epoch boundary block moves it without a commit.
     */
    void committed(long commitSlot) {
        if (!isLive() || !keptToCommits)
            return;

        StoreState state = storeStateService.getState(STATE_ID).orElse(null);
        if (state == null || state.getSlot() == null)
            return;

        storeStateService.saveState(state.toBuilder().slot(Math.max(state.getSlot(), commitSlot)).build());
    }

    @EventListener
    public void handlePreBlockSync(PreBlockSyncEvent event) {
        if (properties == null || !properties.isTxInputOwnerAddrEnabled())
            return;

        if (!enabled()) {
            if (warned.compareAndSet(false, true))
                log.warn("store.utxo.tx-input-owner-addr-enabled is ignored: it needs PostgreSQL 16 or later and the "
                        + "store_state table, the store runs on {}", dsl.dialect().family());
            return;
        }

        isLive();
        //At the tip a block's spends are read from address_utxo. A spend saved while this runs may still use the
        //cache: it finds the address or reads it, and a later restart clears what it adds.
        if (!cacheRetired) {
            cacheRetired = true;
            clearCache("the sync has reached the tip");
        }
        if (current || !filling.compareAndSet(false, true))
            return;

        Thread thread = new Thread(() -> {
            try {
                fill();
            } catch (Exception e) {
                log.error("tx_input.owner_addr fill failed. It continues from where it stopped when the sync restarts", e);
            } finally {
                filling.set(false);
            }
        }, "tx-input-owner-addr-fill");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Fills the address of the tx_input rows that miss it, up to the cursor slot at the start, in chunks of about
     * {@link #chunkRows} rows by spent_at_slot, then marks the state current. Each chunk commits with the state's
     * slot moved to its end, so a restart continues after the last finished chunk.
     */
    void fill() {
        long start = System.currentTimeMillis();
        StoreState state = storeStateService.getState(STATE_ID).orElseThrow();
        long from = state.getSlot() != null ? state.getSlot() : -1;
        Long target = cursorSlot();
        if (target == null)
            target = dsl.select(DSL.max(TX_INPUT.SPENT_AT_SLOT)).from(TX_INPUT).fetchOne(0, Long.class);

        long filled = 0;
        if (target != null && from < target) {
            log.info("tx_input.owner_addr: filling the rows spent after slot {} up to slot {} in the background. The sync "
                    + "continues; /addresses/{address}/transactions uses its full query until this is done.", from, target);
            //Statistics for the new column, so the planner knows the rows to fill are all of the chunk
            dsl.execute("analyze " + TX_INPUT.getName());
        }

        long recentFrom = target != null ? target - recentSlots : Long.MAX_VALUE;
        while (target != null && from < target) {
            long chunkFrom = from;
            //Large chunks up to the recent stretch, then that stretch in one small chunk
            long to = from < recentFrom ? Math.min(chunkEnd(from), recentFrom) : target;
            boolean large = from < recentFrom;
            Integer rows = transactionTemplate.execute(status -> {
                //A large chunk is joined by hashing it and reading address_utxo once, never row by row
                if (large) {
                    dsl.execute("set local enable_nestloop = off");
                    dsl.execute("set local work_mem = '" + FILL_WORK_MEM + "'");
                }
                int updated = fillRange(chunkFrom, to);
                storeStateService.saveState(StoreState.builder().stateId(STATE_ID).status(STATUS_FILLING).slot(to).build());
                return updated;
            });
            filled += rows != null ? rows : 0;
            //Frees the old row versions now, so the next chunk reuses their space instead of growing the table
            outsideTransaction("set maintenance_work_mem = '" + VACUUM_MEM + "'", "vacuum " + TX_INPUT.getName(),
                    "reset maintenance_work_mem");
            log.info("tx_input.owner_addr: filled up to slot {} of {}, {} rows so far, {} s elapsed",
                    to, target, filled, (System.currentTimeMillis() - start) / 1000);
            from = to;
        }

        Long now = cursorSlot();
        storeStateService.saveState(StoreState.builder().stateId(STATE_ID).status(STATUS_CURRENT)
                .slot(now != null ? now : target != null ? target : 0L).build());
        current = true;
        //Every row up to here has the address, and this run writes every new one with it
        keptToCommits = true;
        recheck();
        log.info("tx_input.owner_addr is current: {} rows filled in {} s. /addresses/{address}/transactions reads from it "
                + "once its indexes are built (admin-cli apply-indexes).", filled, (System.currentTimeMillis() - start) / 1000);
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

    /** Sets the address of the rows spent in slots (from, to] that do not have it yet. */
    int fillRange(long from, long to) {
        var au = ADDRESS_UTXO.as("au");
        return dsl.update(TX_INPUT)
                .set(TX_INPUT.OWNER_ADDR, au.OWNER_ADDR)
                .from(au)
                .where(TX_INPUT.SPENT_AT_SLOT.gt(from))
                .and(TX_INPUT.SPENT_AT_SLOT.le(to))
                .and(TX_INPUT.OWNER_ADDR.isNull())
                .and(au.TX_HASH.eq(TX_INPUT.TX_HASH))
                .and(au.OUTPUT_INDEX.eq(TX_INPUT.OUTPUT_INDEX))
                .execute();
    }

    /**
     * Runs VACUUM, which PostgreSQL refuses inside a transaction block, on a connection set to autocommit whatever
     * the pool's default is.
     */
    private void outsideTransaction(String... sqls) {
        dsl.connection(connection -> {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                for (String sql : sqls)
                    statement.execute(sql);
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        });
    }

    /**
     * tx_input rows spent from {@code fromSlot} on were saved without the address, by a store with the flag off. If
     * the address was kept that far, it no longer is: the state goes back to filling from before those rows, so
     * readers stop using it, here at once and on other instances within a minute, until a store with the flag on
     * fills them again. Checked once per run: from the first such save on, every later row is saved the same way.
     */
    void savedWithoutAddr(long fromSlot) {
        if (storeStateService == null || dsl.dialect().family() != SQLDialect.POSTGRES
                || !savedWithoutAddrChecked.compareAndSet(false, true))
            return;

        storeStateService.getState(STATE_ID)
                .filter(state -> STATUS_CURRENT.equals(state.getStatus()) || state.getSlot() == null || state.getSlot() >= fromSlot)
                .ifPresent(state -> {
                    Long keptTo = state.getSlot() != null ? Math.min(state.getSlot(), fromSlot - 1) : null;
                    log.warn("tx_input rows are saved without owner_addr from slot {} (store.utxo.tx-input-owner-addr-enabled "
                            + "is off). /addresses/{address}/transactions uses its full query until a store with it on fills "
                            + "the rows after slot {}.", fromSlot, keptTo);
                    storeStateService.saveState(state.toBuilder().status(STATUS_FILLING).slot(keptTo).build());
                    current = false;
                    readable = false;
                });
    }

    /** A rollback to {@code slot}: every row up to the point still has the address, the rows after it are gone. */
    void rolledBackTo(long slot) {
        if (storeStateService == null || dsl.dialect().family() != SQLDialect.POSTGRES)
            return;

        storeStateService.getState(STATE_ID)
                .filter(state -> state.getSlot() != null && state.getSlot() > slot)
                .ifPresent(state -> storeStateService.saveState(state.toBuilder().slot(slot).build()));
    }
}
