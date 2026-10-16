package com.bloxbean.cardano.yaci.store.account.storage.impl;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.config.JooqTestConfig;
import com.bloxbean.cardano.yaci.store.account.domain.BalanceRollbackEvent;
import com.bloxbean.cardano.yaci.store.account.processor.AccountBalanceRollbackProcessor;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.storage.impl.BalanceRollbackFixtures.AddrKey;
import com.bloxbean.cardano.yaci.store.account.storage.impl.BalanceRollbackFixtures.AddrRow;
import com.bloxbean.cardano.yaci.store.account.storage.impl.BalanceRollbackFixtures.StakeRow;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AddressBalanceRepository;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.StakeBalanceRepository;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.account.storage.impl.BalanceRollbackFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the content-aware balance rollback (Gero-Labs/nexus#1351, bloxbean/yaci-store#1244):
 * the {@code _current} tables must end up exactly where a recomputation from the balance history says they
 * should, for large histories, for keys with nothing left at or before the rollback slot, across repeated
 * rollbacks, and after a rollback that died half way and was run again.
 *
 * <p>Runs without the test-managed transaction: the processor tests need real commits and rollbacks, and every
 * test removes the rows it wrote (all keys start with {@link #PREFIX}).
 */
@DataJpaTest
@Import({JooqTestConfig.class})
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/store/h2",
        "spring.flyway.out-of-order=true",
        "spring.datasource.url=jdbc:h2:mem:mydb",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountBalanceRollbackRegressionTest {

    private static final String PREFIX = "rbreg_";
    // Far above the slots the other tests in this context use, so a processor rollback never touches their rows.
    private static final long BASE = 50_000_000L;

    @Autowired
    private AddressBalanceRepository addressBalanceRepository;

    @Autowired
    private StakeBalanceRepository stakeBalanceRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Flyway flyway;

    private DSLContext dsl;
    private AccountBalanceStorage storage;
    private TransactionTemplate tx;

    @BeforeAll
    void migrate() {
        flyway.migrate();
    }

    @BeforeEach
    void setUp() {
        dsl = transactionAwareDsl(dataSource);
        storage = newStorage();
        tx = new TransactionTemplate(transactionManager);
        deleteAll(dsl, PREFIX);
    }

    @AfterEach
    void cleanUp() {
        deleteAll(dsl, PREFIX);
    }

    // ------------------------------------------------------------------ (a) many rows

    @Test
    void addressRefresh_manyRowsPerUnit_matchesCanonicalForEveryRow() {
        Random rnd = new Random(1351L);
        String p = PREFIX + "a_";
        long slot = BASE + 50_000;
        long last = BASE + 100_000;
        String big = p + "dex_script_address";

        List<AddrRow> history = new ArrayList<>();
        Set<String> refreshedUnits = new TreeSet<>();
        int unitCount = 25; // crosses the 20-unit chunk the storage works in
        for (int i = 0; i < unitCount; i++) {
            String unit = String.format("unit_%02d", i);
            refreshedUnits.add(unit);
            int n = 150 + rnd.nextInt(250);
            List<Long> slots;
            if (i % 7 == 3)
                slots = distinctSlots(rnd, n, slot + 1, last);      // nothing at or before the slot
            else if (i % 7 == 5)
                slots = distinctSlots(rnd, n, BASE + 1, slot);      // nothing after the slot
            else
                slots = distinctSlots(rnd, n, BASE + 1, last);
            if (i % 5 == 0 && !slots.contains(slot) && i % 7 != 3) {
                slots = new ArrayList<>(slots);
                slots.add(slot);                                    // a row exactly at the rollback slot
            }
            history.addAll(addrHistory(rnd, big, unit, slots, i % 4 == 0 ? "addr_full_" + big : null));
        }
        // A unit of the same address that is not part of the rollback.
        history.addAll(addrHistory(rnd, big, "unit_not_rolled_back", distinctSlots(rnd, 200, BASE + 1, last), null));
        // Other addresses, also not part of the rollback, with rows on both sides of the slot.
        for (int a = 0; a < 6; a++)
            for (int u = 0; u < 3; u++)
                history.addAll(addrHistory(rnd, p + "other_" + a, "unit_" + u,
                        distinctSlots(rnd, 100, BASE + 1, last), null));

        insertAddrHistory(dsl, history);
        Map<AddrKey, AddrRow> before = latestAddr(history);
        upsertAddrCurrent(dsl, before.values());

        storage.refreshCurrentAddressBalance(big, refreshedUnits, slot);

        Set<AddrKey> refreshed = refreshedUnits.stream().map(u -> new AddrKey(big, u)).collect(Collectors.toSet());
        Map<AddrKey, AddrRow> expected = canonicalAddrCurrent(history, before, refreshed, slot);
        assertSameState("address_balance_current", expected, readAddrCurrent(dsl, p));
        assertEquals(sortedAddr(history), readAddrHistory(dsl, p), "a refresh must not touch address_balance");

        // The data has to exercise every branch, or the comparison above proves little.
        assertTrue(history.size() > 7_000, "history rows: " + history.size());
        assertEquals(4, refreshed.stream().filter(k -> !expected.containsKey(k)).count(), "units removed");
        assertTrue(refreshed.stream().filter(k -> expected.containsKey(k) && !expected.get(k).equals(before.get(k)))
                .count() >= 15, "units moved back");
        assertTrue(refreshed.stream().anyMatch(k -> expected.containsKey(k) && expected.get(k).slot() == slot),
                "a unit kept its row exactly at the slot");
        assertTrue(expected.values().stream().anyMatch(r -> r.addrFull() != null), "addr_full copied");
        assertTrue(expected.values().stream().anyMatch(r -> r.quantity().bitLength() > 63), "numeric(38) copied");
    }

    // ------------------------------------------------------------------ (b) mixed units on one address

    @Test
    void addressRefresh_unitsWithAndWithoutARowAtOrBeforeTheSlot() {
        Random rnd = new Random(2L);
        String p = PREFIX + "b_";
        long s = BASE + 1_000;
        String addr = p + "addr";
        String otherAddr = p + "other";

        List<AddrRow> history = new ArrayList<>();
        history.addAll(addrHistory(rnd, addr, "lovelace", List.of(s - 10, s - 5, s, s + 3, s + 8), null));
        history.addAll(addrHistory(rnd, addr, "tok_before_only", List.of(s - 20), null));
        history.addAll(addrHistory(rnd, addr, "tok_after_only", List.of(s + 1, s + 2), null));
        history.addAll(addrHistory(rnd, addr, "tok_one_after", List.of(s + 100), null));
        history.addAll(addrHistory(rnd, addr, "tok_mixed", List.of(s - 7, s + 50), null));
        history.addAll(addrHistory(rnd, addr, "tok_not_rolled_back", List.of(s - 1, s + 1), null));
        history.addAll(addrHistory(rnd, otherAddr, "lovelace", List.of(s - 1, s + 1), null));

        insertAddrHistory(dsl, history);
        Map<AddrKey, AddrRow> before = latestAddr(history);
        upsertAddrCurrent(dsl, before.values());

        Set<String> units = Set.of("lovelace", "tok_before_only", "tok_after_only", "tok_one_after", "tok_mixed");
        storage.refreshCurrentAddressBalance(addr, units, s);

        Map<AddrKey, AddrRow> actual = readAddrCurrent(dsl, p);
        Set<AddrKey> refreshed = units.stream().map(u -> new AddrKey(addr, u)).collect(Collectors.toSet());
        assertSameState("address_balance_current", canonicalAddrCurrent(history, before, refreshed, s), actual);

        assertEquals(s, actual.get(new AddrKey(addr, "lovelace")).slot());
        assertEquals(s - 20, actual.get(new AddrKey(addr, "tok_before_only")).slot());
        assertEquals(s - 7, actual.get(new AddrKey(addr, "tok_mixed")).slot());
        assertFalse(actual.containsKey(new AddrKey(addr, "tok_after_only")));
        assertFalse(actual.containsKey(new AddrKey(addr, "tok_one_after")));
        assertEquals(s + 1, actual.get(new AddrKey(addr, "tok_not_rolled_back")).slot(), "unit outside the set");
        assertEquals(s + 1, actual.get(new AddrKey(otherAddr, "lovelace")).slot(), "same unit, other address");
    }

    // ------------------------------------------------------------------ (c) stake balances

    @Test
    void stakeRefresh_manyRowsPerAddress_matchesCanonicalForEveryRow() {
        Random rnd = new Random(3L);
        String p = PREFIX + "c_";
        long slot = BASE + 50_000;
        long last = BASE + 100_000;

        List<StakeRow> history = new ArrayList<>();
        List<String> refreshed = new ArrayList<>();
        int count = 30; // crosses the 20-address chunk
        for (int i = 0; i < count; i++) {
            String stake = p + "stake_" + i;
            refreshed.add(stake);
            int n = 100 + rnd.nextInt(200);
            List<Long> slots = switch (i) {
                case 0, 21 -> distinctSlots(rnd, n, slot + 1, last);   // nothing at or before the slot
                case 1 -> {
                    List<Long> l = new ArrayList<>(distinctSlots(rnd, n, BASE + 1, last));
                    if (!l.contains(slot))
                        l.add(slot);                                   // a row exactly at the slot
                    yield l;
                }
                case 2 -> distinctSlots(rnd, n, BASE + 1, slot);       // nothing after the slot
                default -> distinctSlots(rnd, n, BASE + 1, last);
            };
            history.addAll(stakeHistory(rnd, stake, slots));
        }
        for (int i = 0; i < 3; i++)
            history.addAll(stakeHistory(rnd, p + "not_rolled_back_" + i, distinctSlots(rnd, 100, BASE + 1, last)));

        insertStakeHistory(dsl, history);
        Map<String, StakeRow> before = latestStake(history);
        upsertStakeCurrent(dsl, before.values());

        storage.refreshCurrentStakeAddressBalance(refreshed, slot);

        Map<String, StakeRow> expected = canonicalStakeCurrent(history, before, new HashSet<>(refreshed), slot);
        assertSameState("stake_address_balance_current", expected, readStakeCurrent(dsl, p));
        assertEquals(sortedStake(history), readStakeHistory(dsl, p), "a refresh must not touch stake_address_balance");

        assertFalse(expected.containsKey(p + "stake_0"));
        assertFalse(expected.containsKey(p + "stake_21"));
        assertEquals(slot, expected.get(p + "stake_1").slot());
        assertEquals(before.get(p + "stake_2"), expected.get(p + "stake_2"));
        assertTrue(refreshed.stream().filter(a -> expected.containsKey(a) && !expected.get(a).equals(before.get(a)))
                .count() >= 20, "addresses moved back");
    }

    // ------------------------------------------------------------------ (d) nothing at or before the slot

    @Test
    void refresh_noRowAtOrBeforeTheSlotForAnyKey_removesThoseCurrentRowsOnly() {
        Random rnd = new Random(4L);
        String p = PREFIX + "d_";
        long s = BASE + 10_000;
        String addr = p + "addr";

        List<AddrRow> addrHist = new ArrayList<>();
        for (String unit : List.of("lovelace", "tok_a", "tok_b"))
            addrHist.addAll(addrHistory(rnd, addr, unit, distinctSlots(rnd, 30, s + 1, s + 5_000), null));
        addrHist.addAll(addrHistory(rnd, p + "untouched", "lovelace", distinctSlots(rnd, 30, s + 1, s + 5_000), null));
        List<StakeRow> stakeHist = new ArrayList<>();
        for (String stake : List.of(p + "stake_1", p + "stake_2", p + "stake_untouched"))
            stakeHist.addAll(stakeHistory(rnd, stake, distinctSlots(rnd, 30, s + 1, s + 5_000)));

        insertAddrHistory(dsl, addrHist);
        insertStakeHistory(dsl, stakeHist);
        Map<AddrKey, AddrRow> addrBefore = latestAddr(addrHist);
        Map<String, StakeRow> stakeBefore = latestStake(stakeHist);
        upsertAddrCurrent(dsl, addrBefore.values());
        upsertStakeCurrent(dsl, stakeBefore.values());

        storage.refreshCurrentAddressBalance(addr, Set.of("lovelace", "tok_a", "tok_b"), s);
        storage.refreshCurrentStakeAddressBalance(List.of(p + "stake_1", p + "stake_2"), s);

        Map<AddrKey, AddrRow> addrActual = readAddrCurrent(dsl, p);
        Map<String, StakeRow> stakeActual = readStakeCurrent(dsl, p);
        assertEquals(Map.of(new AddrKey(p + "untouched", "lovelace"), addrBefore.get(new AddrKey(p + "untouched", "lovelace"))),
                addrActual);
        assertEquals(Map.of(p + "stake_untouched", stakeBefore.get(p + "stake_untouched")), stakeActual);
        assertEquals(sortedAddr(addrHist), readAddrHistory(dsl, p));
        assertEquals(sortedStake(stakeHist), readStakeHistory(dsl, p));
    }

    // ------------------------------------------------------------------ (e) repeated rollback via the processor

    @Test
    void processorRollback_sameSlotTwiceThenDeeperThenAfterAFork_matchesCanonicalEachTime() {
        Random rnd = new Random(5L);
        String p = PREFIX + "e_";
        long last = BASE + 20_000;
        long s1 = BASE + 15_000;
        long s2 = BASE + 8_000;

        Chain chain = new Chain(p);
        for (int a = 0; a < 4; a++)
            for (int u = 0; u < 4; u++)
                chain.addrHistory.addAll(addrHistory(rnd, p + "addr_" + a, "unit_" + u,
                        distinctSlots(rnd, 60 + rnd.nextInt(60), BASE + 1, last), null));
        // Units that only exist after s1, after s2, and only before s2 (never part of a rollback).
        chain.addrHistory.addAll(addrHistory(rnd, p + "addr_0", "only_after_s1", distinctSlots(rnd, 20, s1 + 1, last), null));
        chain.addrHistory.addAll(addrHistory(rnd, p + "addr_1", "only_after_s2", distinctSlots(rnd, 20, s2 + 1, last), null));
        chain.addrHistory.addAll(addrHistory(rnd, p + "addr_quiet", "lovelace", distinctSlots(rnd, 20, BASE + 1, s2), null));
        for (int i = 0; i < 25; i++)
            chain.stakeHistory.addAll(stakeHistory(rnd, p + "stake_" + i,
                    distinctSlots(rnd, 50 + rnd.nextInt(100), BASE + 1, last)));
        chain.stakeHistory.addAll(stakeHistory(rnd, p + "stake_only_after_s1", distinctSlots(rnd, 20, s1 + 1, last)));
        chain.stakeHistory.addAll(stakeHistory(rnd, p + "stake_only_after_s2", distinctSlots(rnd, 20, s2 + 1, last)));
        chain.stakeHistory.addAll(stakeHistory(rnd, p + "stake_quiet", distinctSlots(rnd, 20, BASE + 1, s2)));
        chain.seed();
        // A current row with no history at all: no rollback can name it, so it has to survive all of them.
        AddrRow orphan = addrRow(p + "addr_orphan", "lovelace", BASE + 5, BigInteger.valueOf(777), null);
        upsertAddrCurrent(dsl, List.of(orphan));
        chain.addrCurrent.put(orphan.key(), orphan);

        AccountBalanceRollbackProcessor processor = processor(storage);

        Step first = chain.rollbackAndVerify(processor, s1);
        assertTrue(first.addrKeys() >= 16 && first.stakeKeys() >= 25, "first rollback refreshed " + first);
        assertTrue(first.addrRemoved() >= 1 && first.stakeRemoved() >= 1, "first rollback removed " + first);

        Step again = chain.rollbackAndVerify(processor, s1);
        assertEquals(new Step(0, 0, 0, 0), again, "a second rollback to the same slot has nothing to do");

        Step deeper = chain.rollbackAndVerify(processor, s2);
        assertTrue(deeper.addrKeys() >= 16 && deeper.stakeKeys() >= 25, "deeper rollback refreshed " + deeper);
        assertTrue(deeper.addrRemoved() >= 1 && deeper.stakeRemoved() >= 1, "deeper rollback removed " + deeper);

        // The chain moves on along a new fork, touching keys with and without a row at or before s2, then
        // that fork is rolled back as well.
        List<AddrRow> forkAddr = new ArrayList<>();
        forkAddr.addAll(addrHistory(rnd, p + "addr_0", "unit_0", List.of(s2 + 10, s2 + 900), null));
        forkAddr.addAll(addrHistory(rnd, p + "addr_1", "only_after_s2", List.of(s2 + 15), null));
        forkAddr.addAll(addrHistory(rnd, p + "addr_fork_new", "lovelace", List.of(s2 + 20), null));
        List<StakeRow> forkStake = new ArrayList<>();
        forkStake.addAll(stakeHistory(rnd, p + "stake_0", List.of(s2 + 10, s2 + 900)));
        forkStake.addAll(stakeHistory(rnd, p + "stake_only_after_s2", List.of(s2 + 15)));
        chain.apply(forkAddr, forkStake);

        Step fork = chain.rollbackAndVerify(processor, s2);
        assertEquals(new Step(3, 2, 2, 1), fork, "fork rollback");
    }

    // ------------------------------------------------------------------ (f) interrupted and restarted

    @ParameterizedTest
    @ValueSource(strings = {"refreshCurrentAddressBalance:2", "deleteStakeAddressBalanceBySlotGreaterThan:1"})
    void interruptedRollback_leavesHistoryAndCurrentUntouched_andARerunConverges(String failAt) {
        String failMethod = failAt.split(":")[0];
        int failOnCall = Integer.parseInt(failAt.split(":")[1]);

        Random rnd = new Random(6L);
        String p = PREFIX + "f_";
        long last = BASE + 10_000;
        long s = BASE + 6_000;

        Chain chain = new Chain(p);
        for (int a = 0; a < 5; a++)
            for (int u = 0; u < 3; u++)
                chain.addrHistory.addAll(addrHistory(rnd, p + "addr_" + a, "unit_" + u,
                        distinctSlots(rnd, 50, BASE + 1, last), null));
        chain.addrHistory.addAll(addrHistory(rnd, p + "addr_0", "only_after", distinctSlots(rnd, 10, s + 1, last), null));
        for (int i = 0; i < 10; i++)
            chain.stakeHistory.addAll(stakeHistory(rnd, p + "stake_" + i, distinctSlots(rnd, 50, BASE + 1, last)));
        chain.seed();

        List<AddrRow> addrHistBefore = readAddrHistory(dsl, p);
        Map<AddrKey, AddrRow> addrCurBefore = readAddrCurrent(dsl, p);
        List<StakeRow> stakeHistBefore = readStakeHistory(dsl, p);
        Map<String, StakeRow> stakeCurBefore = readStakeCurrent(dsl, p);

        // The process dies part way through the rollback transaction. What the transaction had already written
        // is read back from inside it at that moment, to show the attempt did change both current tables.
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Map<AddrKey, AddrRow>> addrCurAtCrash = new AtomicReference<>();
        AtomicReference<Map<String, StakeRow>> stakeCurAtCrash = new AtomicReference<>();
        AccountBalanceStorage crashing = (AccountBalanceStorage) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{AccountBalanceStorage.class},
                (proxy, method, args) -> {
                    if (method.getName().equals(failMethod) && calls.incrementAndGet() == failOnCall) {
                        addrCurAtCrash.set(readAddrCurrent(dsl, p));
                        stakeCurAtCrash.set(readStakeCurrent(dsl, p));
                        throw new SimulatedCrash();
                    }
                    try {
                        return method.invoke(storage, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });

        assertThrows(SimulatedCrash.class, () -> rollback(processor(crashing), s));

        assertEquals(failOnCall, calls.get(), "the crash fired");
        assertNotEquals(addrCurBefore, addrCurAtCrash.get(), "the attempt had rewritten address_balance_current");
        if (failMethod.startsWith("delete"))
            assertNotEquals(stakeCurBefore, stakeCurAtCrash.get(), "the attempt had rewritten stake_address_balance_current");

        assertEquals(addrHistBefore, readAddrHistory(dsl, p), "address_balance after the failed attempt");
        assertEquals(addrCurBefore, readAddrCurrent(dsl, p), "address_balance_current after the failed attempt");
        assertEquals(stakeHistBefore, readStakeHistory(dsl, p), "stake_address_balance after the failed attempt");
        assertEquals(stakeCurBefore, readStakeCurrent(dsl, p), "stake_address_balance_current after the failed attempt");

        // Restarted: the same rollback runs again from the untouched state and lands on the canonical one.
        Step rerun = chain.rollbackAndVerify(processor(storage), s);
        assertTrue(rerun.addrKeys() >= 15 && rerun.stakeKeys() >= 10 && rerun.addrRemoved() == 1, "rerun " + rerun);
    }

    // ------------------------------------------------------------------ helpers

    private AccountBalanceStorage newStorage() {
        return new AccountBalanceStorageImpl(addressBalanceRepository, stakeBalanceRepository, dsl,
                new StoreProperties(), new AccountStoreProperties());
    }

    /**
     * The processor as it runs in production with content-aware rollback and current balances on. Spring
     * publishes events synchronously, so the BalanceRollbackEvent listener runs inside handleRollback's
     * transaction, before the history rows are deleted.
     */
    private AccountBalanceRollbackProcessor processor(AccountBalanceStorage balanceStorage) {
        AccountStoreProperties properties = AccountStoreProperties.builder()
                .contentAwareRollback(true)
                .currentBalanceEnabled(true)
                .build();
        AtomicReference<AccountBalanceRollbackProcessor> self = new AtomicReference<>();
        ApplicationEventPublisher publisher = event -> {
            if (event instanceof BalanceRollbackEvent balanceRollbackEvent)
                self.get().handleCurrentAddressBalanceForRollback(balanceRollbackEvent);
        };
        self.set(new AccountBalanceRollbackProcessor(balanceStorage, properties, publisher));
        return self.get();
    }

    /** handleRollback is @Transactional in production; here the transaction is opened explicitly. */
    private void rollback(AccountBalanceRollbackProcessor processor, long slot) {
        RollbackEvent event = RollbackEvent.builder()
                .rollbackTo(new Point(slot, "rollback-" + slot))
                .currentPoint(new Point(BASE + 1_000_000, "tip"))
                .currentBlock(BASE + 1_000_000)
                .build();
        tx.executeWithoutResult(status -> processor.handleRollback(event));
    }

    private record Step(int addrKeys, int addrRemoved, int stakeKeys, int stakeRemoved) {
    }

    /** History and current tables for one test's keys, kept in Java alongside the database. */
    private class Chain {
        final String prefix;
        List<AddrRow> addrHistory = new ArrayList<>();
        List<StakeRow> stakeHistory = new ArrayList<>();
        Map<AddrKey, AddrRow> addrCurrent = new HashMap<>();
        Map<String, StakeRow> stakeCurrent = new HashMap<>();

        Chain(String prefix) {
            this.prefix = prefix;
        }

        void seed() {
            insertAddrHistory(dsl, addrHistory);
            insertStakeHistory(dsl, stakeHistory);
            addrCurrent.putAll(latestAddr(addrHistory));
            stakeCurrent.putAll(latestStake(stakeHistory));
            upsertAddrCurrent(dsl, addrCurrent.values());
            upsertStakeCurrent(dsl, stakeCurrent.values());
        }

        /** New blocks: history rows appended, and each touched key's current row moved to its newest. */
        void apply(List<AddrRow> addr, List<StakeRow> stake) {
            insertAddrHistory(dsl, addr);
            insertStakeHistory(dsl, stake);
            addrHistory = new ArrayList<>(addrHistory);
            addrHistory.addAll(addr);
            stakeHistory = new ArrayList<>(stakeHistory);
            stakeHistory.addAll(stake);
            Map<AddrKey, AddrRow> addrLatest = latestAddr(addr);
            Map<String, StakeRow> stakeLatest = latestStake(stake);
            upsertAddrCurrent(dsl, addrLatest.values());
            upsertStakeCurrent(dsl, stakeLatest.values());
            addrCurrent.putAll(addrLatest);
            stakeCurrent.putAll(stakeLatest);
        }

        Step rollbackAndVerify(AccountBalanceRollbackProcessor processor, long slot) {
            Set<AddrKey> addrKeys = keysAfter(addrHistory, slot, AddrRow::key, AddrRow::slot);
            Set<String> stakeKeys = keysAfter(stakeHistory, slot, StakeRow::address, StakeRow::slot);
            Map<AddrKey, AddrRow> expectedAddr = canonicalAddrCurrent(addrHistory, addrCurrent, addrKeys, slot);
            Map<String, StakeRow> expectedStake = canonicalStakeCurrent(stakeHistory, stakeCurrent, stakeKeys, slot);
            List<AddrRow> expectedAddrHistory = sortedAddr(addrHistory.stream().filter(r -> r.slot() <= slot).toList());
            List<StakeRow> expectedStakeHistory = sortedStake(stakeHistory.stream().filter(r -> r.slot() <= slot).toList());

            rollback(processor, slot);

            String at = " after rollback to " + slot;
            assertEquals(expectedAddrHistory, readAddrHistory(dsl, prefix), "address_balance" + at);
            assertEquals(expectedStakeHistory, readStakeHistory(dsl, prefix), "stake_address_balance" + at);
            assertSameState("address_balance_current" + at, expectedAddr, readAddrCurrent(dsl, prefix));
            assertSameState("stake_address_balance_current" + at, expectedStake, readStakeCurrent(dsl, prefix));

            Step step = new Step(addrKeys.size(),
                    (int) addrKeys.stream().filter(k -> addrCurrent.containsKey(k) && !expectedAddr.containsKey(k)).count(),
                    stakeKeys.size(),
                    (int) stakeKeys.stream().filter(k -> stakeCurrent.containsKey(k) && !expectedStake.containsKey(k)).count());

            addrHistory = expectedAddrHistory;
            stakeHistory = expectedStakeHistory;
            addrCurrent = expectedAddr;
            stakeCurrent = expectedStake;
            return step;
        }
    }

    private static final class SimulatedCrash extends RuntimeException {
        SimulatedCrash() {
            super("simulated crash part way through the rollback");
        }
    }
}
