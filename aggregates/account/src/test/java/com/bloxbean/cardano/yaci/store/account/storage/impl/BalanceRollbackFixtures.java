package com.bloxbean.cardano.yaci.store.account.storage.impl;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test data and a canonical, in-Java model of what a balance rollback must leave behind, for the rollback
 * regression tests. The model is deliberately independent of the SQL under test: for every key the current
 * table must hold the newest history row at or before the rollback slot (all columns, copied as is), a key
 * with no such row must be absent, and a key outside the rollback set must keep whatever it had.
 */
final class BalanceRollbackFixtures {

    private static final LocalDateTime TIME_ZERO = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final int INSERT_CHUNK = 500;

    private BalanceRollbackFixtures() {
    }

    record AddrKey(String address, String unit) {
    }

    record AddrRow(String address, String unit, long slot, BigInteger quantity, String addrFull, Long block,
                   Long blockTime, Integer epoch, LocalDateTime updateDatetime) {
        AddrKey key() {
            return new AddrKey(address, unit);
        }
    }

    record StakeRow(String address, long slot, BigInteger quantity, Long block, Long blockTime, Integer epoch,
                    LocalDateTime updateDatetime) {
    }

    /**
     * A DSLContext that joins the caller's Spring transaction, as the jOOQ starter's context does in production.
     * The context from JooqTestConfig takes a fresh auto-commit connection per statement, so it can never be
     * rolled back.
     */
    static DSLContext transactionAwareDsl(DataSource dataSource) {
        return DSL.using(new TransactionAwareDataSourceProxy(dataSource), SQLDialect.H2,
                new Settings().withRenderQuotedNames(RenderQuotedNames.NEVER));
    }

    // ------------------------------------------------------------------ row generation

    static AddrRow addrRow(String address, String unit, long slot, BigInteger quantity, String addrFull) {
        return new AddrRow(address, unit, slot, quantity, addrFull, slot / 20, 1_600_000_000L + slot,
                (int) (slot / 432_000), TIME_ZERO.plusSeconds(slot));
    }

    static StakeRow stakeRow(String address, long slot, BigInteger quantity) {
        return new StakeRow(address, slot, quantity, slot / 20, 1_600_000_000L + slot, (int) (slot / 432_000),
                TIME_ZERO.plusSeconds(slot));
    }

    /** Mostly lovelace-sized amounts, with some above Long.MAX_VALUE to exercise the numeric(38) copy. */
    static BigInteger randomQuantity(Random rnd) {
        return rnd.nextInt(10) == 0 ? new BigInteger(120, rnd) : BigInteger.valueOf(rnd.nextLong(1, 1_000_000_000_000L));
    }

    /** {@code count} distinct slots in [from, to], ascending. */
    static List<Long> distinctSlots(Random rnd, int count, long from, long to) {
        if (to - from + 1 < count)
            throw new IllegalArgumentException("range too small");
        TreeSet<Long> slots = new TreeSet<>();
        while (slots.size() < count)
            slots.add(rnd.nextLong(from, to + 1));
        return new ArrayList<>(slots);
    }

    static List<AddrRow> addrHistory(Random rnd, String address, String unit, List<Long> slots, String addrFull) {
        return slots.stream().map(s -> addrRow(address, unit, s, randomQuantity(rnd), addrFull)).toList();
    }

    static List<StakeRow> stakeHistory(Random rnd, String address, List<Long> slots) {
        return slots.stream().map(s -> stakeRow(address, s, randomQuantity(rnd))).toList();
    }

    // ------------------------------------------------------------------ database access

    static void insertAddrHistory(DSLContext dsl, Collection<AddrRow> rows) {
        var t = ADDRESS_BALANCE;
        for (List<AddrRow> chunk : chunks(rows)) {
            var insert = dsl.insertInto(t, t.ADDRESS, t.UNIT, t.SLOT, t.QUANTITY, t.ADDR_FULL, t.BLOCK, t.BLOCK_TIME,
                    t.EPOCH, t.UPDATE_DATETIME);
            for (AddrRow r : chunk)
                insert = insert.values(r.address(), r.unit(), r.slot(), r.quantity(), r.addrFull(), r.block(),
                        r.blockTime(), r.epoch(), r.updateDatetime());
            insert.execute();
        }
    }

    /** Upsert, as the block processor does when it moves a key's current balance forward. */
    static void upsertAddrCurrent(DSLContext dsl, Collection<AddrRow> rows) {
        var t = ADDRESS_BALANCE_CURRENT;
        for (AddrRow r : rows) {
            dsl.deleteFrom(t).where(t.ADDRESS.eq(r.address())).and(t.UNIT.eq(r.unit())).execute();
        }
        for (List<AddrRow> chunk : chunks(rows)) {
            var insert = dsl.insertInto(t, t.ADDRESS, t.UNIT, t.SLOT, t.QUANTITY, t.ADDR_FULL, t.BLOCK, t.BLOCK_TIME,
                    t.EPOCH, t.UPDATE_DATETIME);
            for (AddrRow r : chunk)
                insert = insert.values(r.address(), r.unit(), r.slot(), r.quantity(), r.addrFull(), r.block(),
                        r.blockTime(), r.epoch(), r.updateDatetime());
            insert.execute();
        }
    }

    static void insertStakeHistory(DSLContext dsl, Collection<StakeRow> rows) {
        var t = STAKE_ADDRESS_BALANCE;
        for (List<StakeRow> chunk : chunks(rows)) {
            var insert = dsl.insertInto(t, t.ADDRESS, t.SLOT, t.QUANTITY, t.BLOCK, t.BLOCK_TIME, t.EPOCH,
                    t.UPDATE_DATETIME);
            for (StakeRow r : chunk)
                insert = insert.values(r.address(), r.slot(), r.quantity(), r.block(), r.blockTime(), r.epoch(),
                        r.updateDatetime());
            insert.execute();
        }
    }

    static void upsertStakeCurrent(DSLContext dsl, Collection<StakeRow> rows) {
        var t = STAKE_ADDRESS_BALANCE_CURRENT;
        for (StakeRow r : rows) {
            dsl.deleteFrom(t).where(t.ADDRESS.eq(r.address())).execute();
        }
        for (List<StakeRow> chunk : chunks(rows)) {
            var insert = dsl.insertInto(t, t.ADDRESS, t.SLOT, t.QUANTITY, t.BLOCK, t.BLOCK_TIME, t.EPOCH,
                    t.UPDATE_DATETIME);
            for (StakeRow r : chunk)
                insert = insert.values(r.address(), r.slot(), r.quantity(), r.block(), r.blockTime(), r.epoch(),
                        r.updateDatetime());
            insert.execute();
        }
    }

    static List<AddrRow> readAddrHistory(DSLContext dsl, String prefix) {
        var t = ADDRESS_BALANCE;
        return dsl.selectFrom(t).where(t.ADDRESS.startsWith(prefix))
                .orderBy(t.ADDRESS, t.UNIT, t.SLOT)
                .fetch(r -> new AddrRow(r.getAddress(), r.getUnit(), r.getSlot(), r.getQuantity(), r.getAddrFull(),
                        r.getBlock(), r.getBlockTime(), r.getEpoch(), r.getUpdateDatetime()));
    }

    static Map<AddrKey, AddrRow> readAddrCurrent(DSLContext dsl, String prefix) {
        var t = ADDRESS_BALANCE_CURRENT;
        Map<AddrKey, AddrRow> current = new HashMap<>();
        dsl.selectFrom(t).where(t.ADDRESS.startsWith(prefix))
                .fetch(r -> new AddrRow(r.getAddress(), r.getUnit(), r.getSlot(), r.getQuantity(), r.getAddrFull(),
                        r.getBlock(), r.getBlockTime(), r.getEpoch(), r.getUpdateDatetime()))
                .forEach(r -> current.put(r.key(), r));
        return current;
    }

    static List<StakeRow> readStakeHistory(DSLContext dsl, String prefix) {
        var t = STAKE_ADDRESS_BALANCE;
        return dsl.selectFrom(t).where(t.ADDRESS.startsWith(prefix))
                .orderBy(t.ADDRESS, t.SLOT)
                .fetch(r -> new StakeRow(r.getAddress(), r.getSlot(), r.getQuantity(), r.getBlock(), r.getBlockTime(),
                        r.getEpoch(), r.getUpdateDatetime()));
    }

    static Map<String, StakeRow> readStakeCurrent(DSLContext dsl, String prefix) {
        var t = STAKE_ADDRESS_BALANCE_CURRENT;
        Map<String, StakeRow> current = new HashMap<>();
        dsl.selectFrom(t).where(t.ADDRESS.startsWith(prefix))
                .fetch(r -> new StakeRow(r.getAddress(), r.getSlot(), r.getQuantity(), r.getBlock(), r.getBlockTime(),
                        r.getEpoch(), r.getUpdateDatetime()))
                .forEach(r -> current.put(r.address(), r));
        return current;
    }

    static void deleteAll(DSLContext dsl, String prefix) {
        dsl.deleteFrom(ADDRESS_BALANCE).where(ADDRESS_BALANCE.ADDRESS.startsWith(prefix)).execute();
        dsl.deleteFrom(ADDRESS_BALANCE_CURRENT).where(ADDRESS_BALANCE_CURRENT.ADDRESS.startsWith(prefix)).execute();
        dsl.deleteFrom(STAKE_ADDRESS_BALANCE).where(STAKE_ADDRESS_BALANCE.ADDRESS.startsWith(prefix)).execute();
        dsl.deleteFrom(STAKE_ADDRESS_BALANCE_CURRENT)
                .where(STAKE_ADDRESS_BALANCE_CURRENT.ADDRESS.startsWith(prefix)).execute();
    }

    // ------------------------------------------------------------------ canonical model

    /** The newest row per key, i.e. the current table at the tip. */
    static <K, R> Map<K, R> latestPerKey(Collection<R> history, Function<R, K> key, ToLongFunction<R> slot) {
        Map<K, R> latest = new HashMap<>();
        for (R row : history)
            latest.merge(key.apply(row), row, (a, b) -> slot.applyAsLong(a) >= slot.applyAsLong(b) ? a : b);
        return latest;
    }

    /**
     * What the current table must hold after refreshing {@code refreshed} at {@code rollbackSlot}: for each of
     * those keys, its newest history row at or before the slot, or nothing; every other key as it was.
     */
    static <K, R> Map<K, R> canonicalCurrent(Collection<R> history, Map<K, R> currentBefore, Set<K> refreshed,
                                             long rollbackSlot, Function<R, K> key, ToLongFunction<R> slot) {
        Map<K, R> expected = new HashMap<>(currentBefore);
        Map<K, R> latestAtOrBefore = latestPerKey(
                history.stream().filter(r -> slot.applyAsLong(r) <= rollbackSlot).toList(), key, slot);
        for (K k : refreshed) {
            R row = latestAtOrBefore.get(k);
            if (row != null)
                expected.put(k, row);
            else
                expected.remove(k);
        }
        return expected;
    }

    /** The keys a content-aware rollback to {@code rollbackSlot} refreshes: every key with a history row after it. */
    static <K, R> Set<K> keysAfter(Collection<R> history, long rollbackSlot, Function<R, K> key, ToLongFunction<R> slot) {
        return history.stream().filter(r -> slot.applyAsLong(r) > rollbackSlot).map(key)
                .collect(Collectors.toCollection(HashSet::new));
    }

    static Map<AddrKey, AddrRow> canonicalAddrCurrent(Collection<AddrRow> history, Map<AddrKey, AddrRow> before,
                                                      Set<AddrKey> refreshed, long rollbackSlot) {
        return canonicalCurrent(history, before, refreshed, rollbackSlot, AddrRow::key, AddrRow::slot);
    }

    static Map<String, StakeRow> canonicalStakeCurrent(Collection<StakeRow> history, Map<String, StakeRow> before,
                                                       Set<String> refreshed, long rollbackSlot) {
        return canonicalCurrent(history, before, refreshed, rollbackSlot, StakeRow::address, StakeRow::slot);
    }

    static Map<AddrKey, AddrRow> latestAddr(Collection<AddrRow> history) {
        return latestPerKey(history, AddrRow::key, AddrRow::slot);
    }

    static Map<String, StakeRow> latestStake(Collection<StakeRow> history) {
        return latestPerKey(history, StakeRow::address, StakeRow::slot);
    }

    static List<AddrRow> sortedAddr(Collection<AddrRow> rows) {
        return rows.stream().sorted(Comparator.comparing(AddrRow::address).thenComparing(AddrRow::unit)
                .thenComparingLong(AddrRow::slot)).toList();
    }

    static List<StakeRow> sortedStake(Collection<StakeRow> rows) {
        return rows.stream().sorted(Comparator.comparing(StakeRow::address).thenComparingLong(StakeRow::slot)).toList();
    }

    /** Every key compared on every column, with the first few differences named when it fails. */
    static <K, R> void assertSameState(String label, Map<K, R> expected, Map<K, R> actual) {
        List<String> diffs = new ArrayList<>();
        Set<K> keys = new LinkedHashSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        for (K k : keys) {
            R e = expected.get(k);
            R a = actual.get(k);
            if (!Objects.equals(e, a))
                diffs.add(k + ": expected " + e + " but was " + a);
        }
        if (!diffs.isEmpty())
            fail(label + ": " + diffs.size() + " of " + keys.size() + " keys differ, e.g.\n  "
                    + String.join("\n  ", diffs.subList(0, Math.min(10, diffs.size()))));
    }

    private static <T> List<List<T>> chunks(Collection<T> rows) {
        List<T> list = new ArrayList<>(rows);
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += INSERT_CHUNK)
            chunks.add(list.subList(i, Math.min(i + INSERT_CHUNK, list.size())));
        return chunks;
    }
}
