package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.cache.RocksDBCache;
import com.bloxbean.cardano.yaci.store.common.cache.RocksDBCacheStore;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.domain.StoreState;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreBlockSyncEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * tx_input.owner_addr on PostgreSQL ({@link TxInputOwnerAddr}) with a RocksDB cache: the address written from the
 * first block, the cache miss read from address_utxo, a spend saved before its output, the fill of a store synced
 * without it, the indexes, the transactions query, and how the state follows gaps and rollbacks. The indexes are
 * built from dbutils index.yml, as admin-cli apply-indexes builds them.
 */
@Testcontainers(disabledWithoutDocker = true)
class TxInputOwnerAddrPostgresIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16");

    private static final Path INDEX_YML = Path.of("../../components/dbutils/src/main/resources/index.yml");
    private static final String ALICE = "addr_test1alice";
    private static final String BOB = "addr_test1bob";

    private static JdbcTemplate jdbc;
    private static DSLContext dsl;
    private static DataSourceTransactionManager transactionManager;

    @TempDir
    Path tmp;

    //-1: the store has no cursor yet, as a new store
    private final AtomicLong cursorSlot = new AtomicLong(-1);
    private final List<RocksDBCacheStore> cacheStores = new ArrayList<>();
    private InMemoryStoreStateService stateService;
    private TxInputOwnerAddr ownerAddr;
    private UtxoStorageImpl storage;
    private UtxoStorageReaderImpl fullQuery;
    private UtxoStorageReaderImpl ownerAddrQuery;

    @BeforeAll
    static void setUpDatabase() {
        DriverManagerDataSource ds = new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Flyway.configure().dataSource(ds)
                .locations("filesystem:src/main/resources/db/store/postgresql")
                .load().migrate();
        jdbc = new JdbcTemplate(ds);
        dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        transactionManager = new DataSourceTransactionManager(ds);
    }

    @BeforeEach
    void setUp() {
        jdbc.update("delete from address_utxo");
        jdbc.update("delete from tx_input");
        jdbc.execute("drop index if exists " + TxInputOwnerAddr.TX_INPUT_INDEX);
        jdbc.execute("drop index if exists " + TxInputOwnerAddr.ADDRESS_UTXO_INDEX);
        stateService = new InMemoryStoreStateService();
        ownerAddr = newOwnerAddr(true);
        storage = newStorage(ownerAddr);
        fullQuery = new UtxoStorageReaderImpl(mock(UtxoRepository.class), dsl);
        ownerAddrQuery = new UtxoStorageReaderImpl(mock(UtxoRepository.class), dsl, () -> ownerAddr.isReadable());
    }

    @AfterEach
    void closeCaches() {
        cacheStores.forEach(RocksDBCacheStore::close);
    }

    /** A run of the store: a new RocksDB cache, empty, as after a restart. */
    private TxInputOwnerAddr newOwnerAddr(boolean enabled) {
        CursorService cursorService = mock(CursorService.class);
        when(cursorService.getCursor()).thenAnswer(invocation -> cursorSlot.get() < 0
                ? Optional.empty() : Optional.of(Cursor.builder().slot(cursorSlot.get()).build()));
        Path folder = tmp.resolve("cache-" + cacheStores.size());
        return new TxInputOwnerAddr(UtxoStoreProperties.builder().txInputOwnerAddrEnabled(enabled).build(), dsl,
                transactionManager, stateService, cursorService, () -> {
            RocksDBCacheStore cacheStore = new RocksDBCacheStore(folder);
            cacheStores.add(cacheStore);
            return cacheStore.cache(TxInputOwnerAddr.CACHE_NAME, UtxoKeyCodec.INSTANCE, RocksDBCache.STRING);
        });
    }

    private UtxoStorageImpl newStorage(TxInputOwnerAddr txInputOwnerAddr) {
        return new UtxoStorageImpl(mock(UtxoRepository.class), mock(TxInputRepository.class), dsl,
                new UtxoCache(), transactionManager, null, txInputOwnerAddr);
    }

    private static String hash(int n) {
        return String.format("%064x", n);
    }

    private static AddressUtxo output(String txHash, int index, String address, long slot) {
        return AddressUtxo.builder()
                .txHash(txHash).outputIndex(index).slot(slot).blockNumber(slot / 10).blockTime(1_600_000L + slot)
                .ownerAddr(address).lovelaceAmount(BigInteger.valueOf(1_000_000L))
                .build();
    }

    private static TxInput spend(String txHash, int index, String spentBy, long atSlot) {
        return TxInput.builder().txHash(txHash).outputIndex(index)
                .spentAtSlot(atSlot).spentAtBlock(atSlot / 10).spentBlockTime(1_600_000L + atSlot).spentTxHash(spentBy).build();
    }

    /** A commit, after which the store's cursor is at the batch's last block. */
    private void commit(long slot) {
        storage.handleCommit(new CommitEvent<>(EventMetadata.builder().slot(slot).build(), List.of()));
        cursorSlot.set(slot);
    }

    /**
     * Alice and Bob over 7 transactions, one batch per transaction, saved as a block saves them: its spends first.
     * Alice's outputs are spent with and without change back to her, two in one transaction.
     */
    private void history() {
        storage.saveUnspent(List.of(output(hash(1), 0, ALICE, 100), output(hash(1), 1, BOB, 100)));
        commit(100);
        // tx2 spends Alice's output, change back to Alice
        storage.saveSpent(List.of(spend(hash(1), 0, hash(2), 200)));
        storage.saveUnspent(List.of(output(hash(2), 0, BOB, 200), output(hash(2), 1, ALICE, 200)));
        commit(200);
        // tx3 spends Alice's change, pays only Bob
        storage.saveSpent(List.of(spend(hash(2), 1, hash(3), 300)));
        storage.saveUnspent(List.of(output(hash(3), 0, BOB, 300)));
        commit(300);
        // tx4 pays Alice twice
        storage.saveSpent(List.of(spend(hash(1), 1, hash(4), 400)));
        storage.saveUnspent(List.of(output(hash(4), 0, ALICE, 400), output(hash(4), 1, ALICE, 400)));
        commit(400);
        // tx5 spends both, pays Bob; tx6 (same slot) only Bob
        storage.saveSpent(List.of(spend(hash(4), 0, hash(5), 500), spend(hash(4), 1, hash(5), 500)));
        storage.saveUnspent(List.of(output(hash(5), 0, BOB, 500)));
        storage.saveSpent(List.of(spend(hash(2), 0, hash(6), 500)));
        storage.saveUnspent(List.of(output(hash(6), 0, BOB, 500)));
        commit(500);
        // tx7 pays Alice
        storage.saveSpent(List.of(spend(hash(5), 0, hash(7), 600)));
        storage.saveUnspent(List.of(output(hash(7), 0, ALICE, 600)));
        commit(600);
    }

    private long rowsWithoutAddr() {
        return jdbc.queryForObject("select count(*) from tx_input where owner_addr is null", Long.class);
    }

    /** Rows whose owner_addr is not the address of the output they spend. */
    private long rowsWithWrongAddr() {
        return jdbc.queryForObject("""
                select count(*) from tx_input i join address_utxo a on a.tx_hash = i.tx_hash and a.output_index = i.output_index
                where i.owner_addr is distinct from a.owner_addr""", Long.class);
    }

    private StoreState state() {
        return stateService.getState(TxInputOwnerAddr.STATE_ID).orElseThrow();
    }

    /** The store reaches the tip: the rows still missing the address are filled and the state becomes CURRENT. */
    private void atTheTipWithoutIndexes() throws InterruptedException {
        ownerAddr.handlePreBlockSync(new PreBlockSyncEvent(null));
        for (int i = 0; i < 300 && !TxInputOwnerAddr.STATUS_CURRENT.equals(state().getStatus()); i++)
            Thread.sleep(100);
        assertThat(state().getStatus()).isEqualTo(TxInputOwnerAddr.STATUS_CURRENT);
    }

    /** At the tip, then the indexes applied: reads use the address. */
    private void atTheTip() throws Exception {
        atTheTipWithoutIndexes();
        applyIndexes();
        ownerAddr.recheck();
        assertThat(ownerAddr.isReadable()).isTrue();
    }

    /** The two indexes as admin-cli apply-indexes builds them from index.yml (IndexService), then ANALYZE. */
    @SuppressWarnings("unchecked")
    private static void applyIndexes() throws Exception {
        List<Map<String, Object>> tables;
        try (InputStream in = Files.newInputStream(INDEX_YML)) {
            tables = new Yaml().load(in);
        }
        int applied = 0;
        for (Map<String, Object> table : tables) {
            for (Map<String, Object> index : (List<Map<String, Object>>) table.get("indexes")) {
                String name = (String) index.get("name");
                if (!name.equals(TxInputOwnerAddr.TX_INPUT_INDEX) && !name.equals(TxInputOwnerAddr.ADDRESS_UTXO_INDEX))
                    continue;
                String where = index.get("where") != null ? " WHERE " + index.get("where") : "";
                jdbc.execute(String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s) %s", name, table.get("table"),
                        String.join(", ", (List<String>) index.get("columns")), where));
                jdbc.execute("ANALYZE " + table.get("table"));
                applied++;
            }
        }
        assertThat(applied).isEqualTo(2);
    }

    private static List<String> txHashes(List<AddressTransaction> transactions) {
        return transactions.stream().map(AddressTransaction::getTxHash).toList();
    }

    @Test
    void aNewStoreWritesTheAddressFromTheFirstBlockAndIsReadOnceTheIndexesAreApplied() throws Exception {
        history();

        assertThat(rowsWithoutAddr()).isZero();
        assertThat(rowsWithWrongAddr()).isZero();
        assertThat(state().getStatus()).isEqualTo(TxInputOwnerAddr.STATUS_FILLING);
        assertThat(state().getSlot()).isEqualTo(600);
        assertThat(ownerAddr.isReadable()).isFalse();

        //At the tip nothing is left to fill, and the store builds no index
        atTheTipWithoutIndexes();
        assertThat(state().getSlot()).isEqualTo(600);
        ownerAddr.recheck();
        assertThat(ownerAddr.isReadable()).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from pg_class where relname in (?, ?)", Long.class,
                TxInputOwnerAddr.ADDRESS_UTXO_INDEX, TxInputOwnerAddr.TX_INPUT_INDEX)).isZero();

        applyIndexes();
        ownerAddr.recheck();
        assertThat(ownerAddr.isReadable()).isTrue();
    }

    @Test
    void aNewStoreWhoseCursorIsAlreadySetStillWritesTheAddressFromTheFirstBlock() throws Exception {
        //A new store's cursor is at the genesis block before the first output or spend is saved
        cursorSlot.set(0);

        history();

        assertThat(rowsWithoutAddr()).isZero();
        assertThat(state().getStatus()).isEqualTo(TxInputOwnerAddr.STATUS_FILLING);
        assertThat(state().getSlot()).isEqualTo(600);

        atTheTip();
        assertThat(state().getSlot()).isEqualTo(600);
    }

    @Test
    void aSpendSavedBeforeItsOutputIsInsertedAtTheCommitWithTheAddress() {
        storage.saveUnspent(List.of(output(hash(1), 0, ALICE, 100)));
        commit(100);

        //A block's spends are saved before its outputs: tx3 spends tx2's output of the same block
        storage.saveSpent(List.of(spend(hash(1), 0, hash(2), 200), spend(hash(2), 0, hash(3), 200)));
        assertThat(jdbc.queryForList("select tx_hash from tx_input", String.class)).containsExactly(hash(1));
        storage.saveUnspent(List.of(output(hash(2), 0, ALICE, 200), output(hash(3), 0, BOB, 200)));
        commit(200);

        assertThat(jdbc.queryForObject("select count(*) from tx_input", Long.class)).isEqualTo(2);
        assertThat(rowsWithoutAddr()).isZero();
        assertThat(rowsWithWrongAddr()).isZero();
        assertThat(state().getSlot()).isEqualTo(200);
    }

    @Test
    void afterARestartTheEmptyCacheFallsBackToAddressUtxo() {
        storage.saveUnspent(List.of(output(hash(1), 0, ALICE, 100), output(hash(1), 1, BOB, 100)));
        commit(100);

        //Restarted: a new, empty cache
        ownerAddr = newOwnerAddr(true);
        storage = newStorage(ownerAddr);
        storage.saveSpent(List.of(spend(hash(1), 0, hash(2), 200), spend(hash(1), 1, hash(2), 200)));
        storage.saveUnspent(List.of(output(hash(2), 0, ALICE, 200)));
        commit(200);

        assertThat(rowsWithoutAddr()).isZero();
        assertThat(rowsWithWrongAddr()).isZero();
        assertThat(state().getSlot()).isEqualTo(200);
    }

    @Test
    void aStoreSyncedWithoutTheAddressIsFilledAtTheTip() throws Exception {
        //Synced with the flag off
        storage = newStorage(newOwnerAddr(false));
        history();
        assertThat(rowsWithoutAddr()).isEqualTo(7);

        //Turned on: new rows get the address, the old ones are filled at the tip, in several chunks
        ownerAddr = newOwnerAddr(true);
        storage = newStorage(ownerAddr);
        storage.saveSpent(List.of(spend(hash(7), 0, hash(8), 700)));
        storage.saveUnspent(List.of(output(hash(8), 0, BOB, 700)));
        commit(700);
        assertThat(rowsWithoutAddr()).isEqualTo(7);
        assertThat(state().getSlot()).isNull();

        ownerAddr.chunkRows = 2;
        ownerAddr.recentSlots = 150;
        atTheTip();

        assertThat(rowsWithoutAddr()).isZero();
        assertThat(rowsWithWrongAddr()).isZero();
        assertThat(state().getStatus()).isEqualTo(TxInputOwnerAddr.STATUS_CURRENT);
        assertThat(state().getSlot()).isEqualTo(700);
    }

    @Test
    void theOwnerAddrQueryReturnsWhatTheFullQueryReturns() throws Exception {
        history();
        atTheTip();

        assertThat(txHashes(ownerAddrQuery.findTransactionsByAddress(ALICE, 0, 10, Order.desc)))
                .containsExactly(hash(7), hash(5), hash(4), hash(3), hash(2), hash(1));

        for (String address : List.of(ALICE, BOB)) {
            for (Order order : Order.values()) {
                List<String> all = txHashes(fullQuery.findTransactionsByAddress(address, 0, 100, order));
                assertThat(txHashes(ownerAddrQuery.findTransactionsByAddress(address, 0, 100, order)))
                        .as("%s %s", address, order).containsExactlyInAnyOrderElementsOf(all);

                //Pages of 2 add up to the same rows, in slot order, with no repeats
                List<AddressTransaction> paged = new ArrayList<>();
                for (int page = 0; page < 5; page++)
                    paged.addAll(ownerAddrQuery.findTransactionsByAddress(address, page, 2, order));
                assertThat(txHashes(paged)).as("%s %s pages", address, order)
                        .doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(all);
                //Blocks here are slot / 10, so block order is slot order
                List<Long> blocks = paged.stream().map(AddressTransaction::getBlockNumber).toList();
                assertThat(blocks).isSortedAccordingTo(order == Order.desc
                        ? Comparator.reverseOrder() : Comparator.naturalOrder());
            }
        }

        AddressTransaction latest = ownerAddrQuery.findTransactionsByAddress(ALICE, 0, 1, Order.desc).get(0);
        assertThat(latest.getBlockNumber()).isEqualTo(60);
        assertThat(latest.getBlockHeight()).isEqualTo(60);
        assertThat(latest.getBlockTime()).isEqualTo(1_600_600L);
    }

    @Test
    void aPageIsReadAlongBothIndexesWithoutReadingTheHistory() throws Exception {
        //20,000 outputs, one in a hundred Alice's, and 19,000 of them spent five slots later
        jdbc.update("""
                insert into address_utxo (tx_hash, output_index, slot, block, block_time, owner_addr)
                select lpad(to_hex(g), 64, '0'), 0, g, g / 10, g, case when g % 100 = 0 then ? else ? end
                from generate_series(1, 20000) g""", ALICE, BOB);
        jdbc.update("""
                insert into tx_input (tx_hash, output_index, spent_at_slot, spent_at_block, spent_block_time, spent_tx_hash, owner_addr)
                select lpad(to_hex(g), 64, '0'), 0, g + 5, (g + 5) / 10, g + 5, lpad(to_hex(g + 100000), 64, '0'),
                       case when g % 100 = 0 then ? else ? end
                from generate_series(1, 19000) g""", ALICE, BOB);
        stateService.saveState(StoreState.builder().stateId(TxInputOwnerAddr.STATE_ID)
                .status(TxInputOwnerAddr.STATUS_FILLING).slot(20005L).build());
        cursorSlot.set(20005);
        atTheTip();

        String plan = new TransactionTemplate(transactionManager).execute(status -> {
            //Small tables: keep the planner on the indexes it would use on a large store
            dsl.execute("set local enable_seqscan = off");
            dsl.execute("set local enable_bitmapscan = off");
            return dsl.explain(ownerAddrQuery.transactionsByOwnerAddr(ALICE, 0, 20, Order.desc)).plan();
        });

        //The halves are merged in order and each stops at its limit: no sort of the whole history
        assertThat(plan)
                .contains("Merge Append")
                .contains("Index Scan Backward using " + TxInputOwnerAddr.ADDRESS_UTXO_INDEX)
                .contains("Index Scan Backward using " + TxInputOwnerAddr.TX_INPUT_INDEX)
                .doesNotContain("Seq Scan")
                .doesNotContainPattern("(?m)(->|^\\|?)\\s*Sort\\s+\\(");
        assertThat(txHashes(ownerAddrQuery.findTransactionsByAddress(ALICE, 0, 20, Order.desc)))
                .containsExactlyElementsOf(txHashes(fullQuery.findTransactionsByAddress(ALICE, 0, 20, Order.desc)));
    }

    @Test
    void blocksSyncedWithTheFlagOffAreFilledAgainBeforeReadsUseIt() throws Exception {
        history();
        atTheTip();

        //The store runs with the flag off: its first spend saved without the address puts the state back to filling
        storage = newStorage(newOwnerAddr(false));
        storage.saveSpent(List.of(spend(hash(7), 0, hash(8), 700)));
        storage.saveUnspent(List.of(output(hash(8), 0, BOB, 700)));
        commit(700);
        assertThat(state().getStatus()).isEqualTo(TxInputOwnerAddr.STATUS_FILLING);
        assertThat(state().getSlot()).isEqualTo(600);

        //Started again with the flag on: not readable until the rows after 600 are filled
        ownerAddr = newOwnerAddr(true);
        assertThat(ownerAddr.isReadable()).isFalse();

        atTheTip();
        assertThat(rowsWithoutAddr()).isZero();
        assertThat(state().getSlot()).isEqualTo(700);
    }

    @Test
    void aStateBehindTheCursorIsNotReadEvenIfCurrent() throws Exception {
        history();
        atTheTip();

        //A version without TxInputOwnerAddr synced on: the state still says CURRENT at 600
        storage = newStorage(null);
        storage.saveSpent(List.of(spend(hash(7), 0, hash(8), 700)));
        commit(700);

        //A read-only instance starting now does not use it
        ownerAddr = newOwnerAddr(true);
        assertThat(ownerAddr.isReadable()).isFalse();
        assertThat(txHashes(ownerAddrQuery.findTransactionsByAddress(ALICE, 0, 100, Order.desc))).contains(hash(8));
    }

    @Test
    void aRollbackMovesTheStateBackToThePoint() throws Exception {
        history();
        atTheTip();

        storage.deleteSpentBySlotGreaterThan(450L);

        assertThat(state().getStatus()).isEqualTo(TxInputOwnerAddr.STATUS_CURRENT);
        assertThat(state().getSlot()).isEqualTo(450);
    }
}
