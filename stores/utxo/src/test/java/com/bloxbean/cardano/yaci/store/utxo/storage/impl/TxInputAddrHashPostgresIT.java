package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * tx_input.addr_hash on PostgreSQL ({@link TxInputAddrHash}): nothing written before the tip, the background fill and
 * its indexes, the hash on insert and at commit, the transactions query, and how the state follows gaps and rollbacks.
 */
@Testcontainers(disabledWithoutDocker = true)
class TxInputAddrHashPostgresIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16");

    private static final String ALICE = "addr_test1alice";
    private static final String BOB = "addr_test1bob";

    private static JdbcTemplate jdbc;
    private static DSLContext dsl;
    private static DataSourceTransactionManager transactionManager;

    private final AtomicLong cursorSlot = new AtomicLong();
    private InMemoryStoreStateService stateService;
    private TxInputAddrHash addrHash;
    private UtxoStorageImpl storage;
    private UtxoStorageReaderImpl fullQuery;
    private UtxoStorageReaderImpl hashQuery;

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
        jdbc.execute("drop index if exists " + TxInputAddrHash.TX_INPUT_INDEX);
        jdbc.execute("drop index if exists " + TxInputAddrHash.ADDRESS_UTXO_INDEX);
        stateService = new InMemoryStoreStateService();
        addrHash = newAddrHash();
        storage = new UtxoStorageImpl(mock(UtxoRepository.class), mock(TxInputRepository.class), dsl,
                new UtxoCache(), transactionManager, null, addrHash);
        fullQuery = new UtxoStorageReaderImpl(mock(UtxoRepository.class), dsl);
        hashQuery = new UtxoStorageReaderImpl(mock(UtxoRepository.class), dsl, () -> addrHash.readableKeptSlot());
    }

    private TxInputAddrHash newAddrHash() {
        CursorService cursorService = mock(CursorService.class);
        org.mockito.Mockito.when(cursorService.getCursor())
                .thenAnswer(invocation -> Optional.of(Cursor.builder().slot(cursorSlot.get()).build()));
        return new TxInputAddrHash(UtxoStoreProperties.builder().txInputAddrHashEnabled(true).build(), dsl,
                transactionManager, stateService, cursorService);
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

    private void commit(long slot) {
        storage.handleCommit(new CommitEvent<>(EventMetadata.builder().slot(slot).build(), List.of()));
    }

    /**
     * Alice and Bob over 7 transactions. Alice's outputs are spent with and without change back to her, two of
     * them in one transaction, and one transaction pays both. Saved the way the initial sync saves them.
     */
    private void history() {
        storage.saveUnspent(List.of(output(hash(1), 0, ALICE, 100), output(hash(1), 1, BOB, 100)));
        // tx2 spends Alice's output, change back to Alice
        storage.saveSpent(List.of(spend(hash(1), 0, hash(2), 200)));
        storage.saveUnspent(List.of(output(hash(2), 0, BOB, 200), output(hash(2), 1, ALICE, 200)));
        // tx3 spends Alice's change, pays only Bob
        storage.saveSpent(List.of(spend(hash(2), 1, hash(3), 300)));
        storage.saveUnspent(List.of(output(hash(3), 0, BOB, 300)));
        // tx4 pays Alice twice
        storage.saveSpent(List.of(spend(hash(1), 1, hash(4), 400)));
        storage.saveUnspent(List.of(output(hash(4), 0, ALICE, 400), output(hash(4), 1, ALICE, 400)));
        // tx5 spends both, pays Bob; tx6 (same slot) only Bob
        storage.saveSpent(List.of(spend(hash(4), 0, hash(5), 500), spend(hash(4), 1, hash(5), 500)));
        storage.saveUnspent(List.of(output(hash(5), 0, BOB, 500)));
        storage.saveSpent(List.of(spend(hash(2), 0, hash(6), 500)));
        storage.saveUnspent(List.of(output(hash(6), 0, BOB, 500)));
        // tx7 pays Alice
        storage.saveSpent(List.of(spend(hash(5), 0, hash(7), 600)));
        storage.saveUnspent(List.of(output(hash(7), 0, ALICE, 600)));
        commit(600);
    }

    private long rowsWithoutHash() {
        return jdbc.queryForObject("select count(*) from tx_input where addr_hash is null", Long.class);
    }

    /** Rows whose hash is not the hash of the address of the output they spend. */
    private long rowsWithWrongHash() {
        return jdbc.queryForObject("""
                select count(*) from tx_input i join address_utxo a on a.tx_hash = i.tx_hash and a.output_index = i.output_index
                where i.addr_hash is distinct from hashtextextended(a.owner_addr, 0)""", Long.class);
    }

    private void fillAtTheTip() throws InterruptedException {
        addrHash.handlePreBlockSync(new PreBlockSyncEvent(null));
        for (int i = 0; i < 300 && !addrHash.isReadable(); i++)
            Thread.sleep(100);
        assertThat(addrHash.isReadable()).isTrue();
    }

    private static List<String> txHashes(List<AddressTransaction> transactions) {
        return transactions.stream().map(AddressTransaction::getTxHash).toList();
    }

    @Test
    void beforeTheTipNothingIsWrittenAndTheFullQueryIsUsed() {
        history();

        assertThat(addrHash.isLive()).isFalse();
        assertThat(addrHash.isReadable()).isFalse();
        assertThat(rowsWithoutHash()).isEqualTo(7);
        assertThat(stateService.getState(TxInputAddrHash.STATE_ID)).isEmpty();
    }

    @Test
    void theFillAtTheTipHashesEveryRowBuildsTheIndexesAndSwitchesReadsOver() throws Exception {
        history();
        cursorSlot.set(600);
        addrHash.chunkRows = 2;      // several chunks
        addrHash.recentSlots = 150;  // large chunks, then the recent stretch

        fillAtTheTip();

        assertThat(rowsWithoutHash()).isZero();
        assertThat(rowsWithWrongHash()).isZero();
        assertThat(jdbc.queryForList("select c.relname from pg_index i join pg_class c on c.oid = i.indexrelid "
                + "where i.indisvalid and c.relname like 'idx_%addr_hash%' order by 1", String.class))
                .containsExactly(TxInputAddrHash.ADDRESS_UTXO_INDEX, TxInputAddrHash.TX_INPUT_INDEX);
        StoreState state = stateService.getState(TxInputAddrHash.STATE_ID).orElseThrow();
        assertThat(state.getStatus()).isEqualTo(TxInputAddrHash.STATUS_CURRENT);
        assertThat(state.getSlot()).isEqualTo(600);
    }

    @Test
    void theHashQueryReturnsWhatTheFullQueryReturns() throws Exception {
        history();
        cursorSlot.set(600);
        fillAtTheTip();

        assertThat(txHashes(hashQuery.findTransactionsByAddress(ALICE, 0, 10, Order.desc)))
                .containsExactly(hash(7), hash(5), hash(4), hash(3), hash(2), hash(1));

        for (String address : List.of(ALICE, BOB)) {
            for (Order order : Order.values()) {
                List<String> all = txHashes(fullQuery.findTransactionsByAddress(address, 0, 100, order));
                assertThat(txHashes(hashQuery.findTransactionsByAddress(address, 0, 100, order)))
                        .as("%s %s", address, order).containsExactlyInAnyOrderElementsOf(all);

                //Pages of 2 add up to the same rows, in slot order, with no repeats
                List<AddressTransaction> paged = new ArrayList<>();
                for (int page = 0; page < 5; page++)
                    paged.addAll(hashQuery.findTransactionsByAddress(address, page, 2, order));
                assertThat(txHashes(paged)).as("%s %s pages", address, order)
                        .doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(all);
                //Blocks here are slot / 10, so block order is slot order
                List<Long> blocks = paged.stream().map(AddressTransaction::getBlockNumber).toList();
                assertThat(blocks).isSortedAccordingTo(order == Order.desc
                        ? java.util.Comparator.reverseOrder() : java.util.Comparator.naturalOrder());
            }
        }

        AddressTransaction latest = hashQuery.findTransactionsByAddress(ALICE, 0, 1, Order.desc).get(0);
        assertThat(latest.getBlockNumber()).isEqualTo(60);
        assertThat(latest.getBlockHeight()).isEqualTo(60);
        assertThat(latest.getBlockTime()).isEqualTo(1_600_600L);
    }

    @Test
    void aPageIsReadAlongBothIndexesWithoutReadingTheHistory() throws Exception {
        //20,000 outputs, Alice's every other one, and 19,000 of them spent five slots later
        jdbc.update("""
                insert into address_utxo (tx_hash, output_index, slot, block, block_time, owner_addr)
                select lpad(to_hex(g), 64, '0'), 0, g, g / 10, g, case when g % 2 = 0 then ? else ? end
                from generate_series(1, 20000) g""", ALICE, BOB);
        jdbc.update("""
                insert into tx_input (tx_hash, output_index, spent_at_slot, spent_at_block, spent_block_time, spent_tx_hash)
                select lpad(to_hex(g), 64, '0'), 0, g + 5, (g + 5) / 10, g + 5, lpad(to_hex(g + 100000), 64, '0')
                from generate_series(1, 19000) g""");
        cursorSlot.set(20005);
        fillAtTheTip();

        String plan = new TransactionTemplate(transactionManager).execute(status -> {
            //Small tables: keep the planner on the indexes it would use on a large store
            dsl.execute("set local enable_seqscan = off");
            dsl.execute("set local enable_bitmapscan = off");
            return dsl.explain(hashQuery.transactionsByAddressHash(ALICE, 20005, 0, 20, Order.desc)).plan();
        });

        assertThat(plan)
                .contains("Index Scan Backward using " + TxInputAddrHash.ADDRESS_UTXO_INDEX)
                .contains("Index Scan Backward using " + TxInputAddrHash.TX_INPUT_INDEX)
                .doesNotContain("Seq Scan");
        assertThat(txHashes(hashQuery.findTransactionsByAddress(ALICE, 0, 20, Order.desc)))
                .containsExactlyElementsOf(txHashes(fullQuery.findTransactionsByAddress(ALICE, 0, 20, Order.desc)));
    }

    @Test
    void anotherAddressWithTheSameHashIsNotReturned() throws Exception {
        history();
        cursorSlot.set(600);
        fillAtTheTip();

        //Bob's spent output tx1#1 made to look like a spend of Alice's: same hash, different owner_addr
        jdbc.update("update tx_input set addr_hash = hashtextextended(?, 0) where tx_hash = ? and output_index = 1", ALICE, hash(1));

        assertThat(txHashes(hashQuery.findTransactionsByAddress(ALICE, 0, 100, Order.desc)))
                .containsExactlyInAnyOrderElementsOf(txHashes(fullQuery.findTransactionsByAddress(ALICE, 0, 100, Order.desc)));
    }

    @Test
    void atTheTipNewSpendsGetTheHashAndTheCommitFillsSpendsSavedBeforeTheirOutput() throws Exception {
        history();
        cursorSlot.set(600);
        fillAtTheTip();

        //The output is already written: the insert looks its address up
        storage.saveSpent(List.of(spend(hash(7), 0, hash(8), 700)));
        assertThat(jdbc.queryForObject("select addr_hash from tx_input where tx_hash = ?", Long.class, hash(7)))
                .isEqualTo(jdbc.queryForObject("select hashtextextended(?, 0)", Long.class, ALICE));

        //Spent before it is saved, as a block's spends are saved before its outputs: filled at the commit
        storage.saveSpent(List.of(spend(hash(9), 0, hash(10), 710)));
        storage.saveUnspent(List.of(output(hash(8), 0, BOB, 700), output(hash(9), 0, ALICE, 705), output(hash(10), 0, BOB, 710)));
        assertThat(jdbc.queryForObject("select addr_hash from tx_input where tx_hash = ?", Long.class, hash(9))).isNull();
        //Visible before the commit (a parallel batch's blocks commit on their own): still found, without its hash
        assertThat(txHashes(hashQuery.findTransactionsByAddress(ALICE, 0, 100, Order.desc)))
                .containsExactlyInAnyOrderElementsOf(txHashes(fullQuery.findTransactionsByAddress(ALICE, 0, 100, Order.desc)))
                .contains(hash(10));
        commit(710);

        assertThat(rowsWithoutHash()).isZero();
        assertThat(rowsWithWrongHash()).isZero();
        assertThat(stateService.getState(TxInputAddrHash.STATE_ID).orElseThrow().getSlot()).isEqualTo(710);
        assertThat(txHashes(hashQuery.findTransactionsByAddress(ALICE, 0, 100, Order.desc)))
                .containsExactlyInAnyOrderElementsOf(txHashes(fullQuery.findTransactionsByAddress(ALICE, 0, 100, Order.desc)))
                .contains(hash(8), hash(9), hash(10));
    }

    @Test
    void blocksSyncedWithTheFlagOffAreFilledAgainBeforeReadsUseIt() throws Exception {
        history();
        cursorSlot.set(600);
        fillAtTheTip();

        //The store runs with the flag off: its first spend saved without the hash puts the state back to filling
        TxInputAddrHash off = new TxInputAddrHash(UtxoStoreProperties.builder().build(), dsl, transactionManager, stateService, null);
        storage = new UtxoStorageImpl(mock(UtxoRepository.class), mock(TxInputRepository.class), dsl,
                new UtxoCache(), transactionManager, null, off);
        storage.saveSpent(List.of(spend(hash(7), 0, hash(8), 700)));
        storage.saveUnspent(List.of(output(hash(8), 0, BOB, 700)));
        cursorSlot.set(700);
        StoreState state = stateService.getState(TxInputAddrHash.STATE_ID).orElseThrow();
        assertThat(state.getStatus()).isEqualTo(TxInputAddrHash.STATUS_FILLING);
        assertThat(state.getSlot()).isEqualTo(600);

        //Started again with the flag on: not readable until the rows after 600 are filled
        addrHash = newAddrHash();
        assertThat(addrHash.isLive()).isTrue();
        assertThat(addrHash.isReadable()).isFalse();

        fillAtTheTip();
        assertThat(rowsWithoutHash()).isZero();
        assertThat(stateService.getState(TxInputAddrHash.STATE_ID).orElseThrow().getSlot()).isEqualTo(700);
    }

    @Test
    void aStateBehindTheCursorIsNotReadEvenIfCurrent() throws Exception {
        history();
        cursorSlot.set(600);
        fillAtTheTip();

        //A version without TxInputAddrHash synced on: the state still says CURRENT at 600
        storage = new UtxoStorageImpl(mock(UtxoRepository.class), mock(TxInputRepository.class), dsl,
                new UtxoCache(), transactionManager, null, null);
        storage.saveSpent(List.of(spend(hash(7), 0, hash(8), 700)));
        cursorSlot.set(700);

        //A read-only instance starting now does not use it
        addrHash = newAddrHash();
        assertThat(addrHash.isReadable()).isFalse();
        assertThat(txHashes(hashQuery.findTransactionsByAddress(ALICE, 0, 100, Order.desc))).contains(hash(8));
    }

    @Test
    void aRollbackMovesTheStateBackToThePoint() throws Exception {
        history();
        cursorSlot.set(600);
        fillAtTheTip();

        storage.deleteSpentBySlotGreaterThan(450L);

        StoreState state = stateService.getState(TxInputAddrHash.STATE_ID).orElseThrow();
        assertThat(state.getStatus()).isEqualTo(TxInputAddrHash.STATUS_CURRENT);
        assertThat(state.getSlot()).isEqualTo(450);
    }
}
