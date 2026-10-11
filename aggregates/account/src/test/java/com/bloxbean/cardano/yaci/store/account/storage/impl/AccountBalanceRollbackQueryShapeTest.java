package com.bloxbean.cardano.yaci.store.account.storage.impl;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.config.JooqTestConfig;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.storage.impl.BalanceRollbackFixtures.AddrRow;
import com.bloxbean.cardano.yaci.store.account.storage.impl.BalanceRollbackFixtures.StakeRow;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AddressBalanceRepository;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.StakeBalanceRepository;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bloxbean.cardano.yaci.store.account.storage.impl.BalanceRollbackFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the shape of the SQL the rollback refresh runs, so the slow form cannot come back unnoticed: the
 * behaviour of a row_number() window over each key's whole history is identical, only its cost is not (193 s
 * against 43 ms for 20 units of one mainnet DEX address, Gero-Labs/nexus#1351). Each key must be one lookup
 * that stops at its first row, and no window function may appear anywhere.
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
class AccountBalanceRollbackQueryShapeTest {

    private static final String PREFIX = "rbshape_";
    private static final long SLOT = 70_000_000L;

    // "limit ?", "fetch next ? rows only", "fetch first ? rows only", "offset ? rows fetch ..." — whatever the
    // dialect renders for a row limit.
    private static final Pattern ROW_LIMIT =
            Pattern.compile("\\blimit\\b|\\bfetch\\s+(first|next)\\b|\\boffset\\b.*\\brows?\\b|\\btop\\b");
    private static final Pattern WINDOW = Pattern.compile("row_number|\\bover\\s*\\(");
    private static final Pattern READS_ADDRESS_HISTORY = Pattern.compile("\\bfrom\\s+address_balance\\b");
    private static final Pattern READS_STAKE_HISTORY = Pattern.compile("\\bfrom\\s+stake_address_balance\\b");

    @Autowired
    private AddressBalanceRepository addressBalanceRepository;

    @Autowired
    private StakeBalanceRepository stakeBalanceRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    private final List<String> executed = new CopyOnWriteArrayList<>();
    private DSLContext dsl;
    private AccountBalanceStorage storage;

    @BeforeAll
    void migrate() {
        flyway.migrate();
    }

    @BeforeEach
    void setUp() {
        dsl = transactionAwareDsl(dataSource);
        ExecuteListener recorder = new ExecuteListener() {
            @Override
            public void executeStart(ExecuteContext ctx) {
                if (ctx.batchSQL().length > 1)
                    executed.addAll(Arrays.asList(ctx.batchSQL()));
                else if (ctx.sql() != null)
                    executed.add(ctx.sql());
            }
        };
        DSLContext recording = dsl.configuration().derive(new DefaultExecuteListenerProvider(recorder)).dsl();
        storage = new AccountBalanceStorageImpl(addressBalanceRepository, stakeBalanceRepository, recording,
                new StoreProperties(), new AccountStoreProperties());
        deleteAll(dsl, PREFIX);
    }

    @AfterEach
    void cleanUp() {
        deleteAll(dsl, PREFIX);
    }

    @Test
    void addressRefresh_isOneBoundedLookupPerUnit_withNoWindowFunction() {
        Random rnd = new Random(7L);
        String address = PREFIX + "addr";
        List<AddrRow> history = new ArrayList<>();
        Set<String> units = new TreeSet<>();
        for (int i = 0; i < 23; i++) { // two chunks of the storage's 20
            String unit = "unit_" + i;
            units.add(unit);
            history.addAll(addrHistory(rnd, address, unit, distinctSlots(rnd, 6, SLOT - 1_000, SLOT + 1_000), null));
        }
        insertAddrHistory(dsl, history);
        upsertAddrCurrent(dsl, latestAddr(history).values());

        executed.clear();
        storage.refreshCurrentAddressBalance(address, units, SLOT);
        List<String> sql = lower(executed);
        System.out.println("refreshCurrentAddressBalance SQL (" + sql.size() + " statements), first: " + sql.get(0));

        assertNoWindowFunction(sql);
        List<String> historyReads = matching(sql, READS_ADDRESS_HISTORY);
        assertEquals(units.size(), historyReads.size(),
                "one address_balance lookup per unit, got:\n" + String.join("\n", historyReads));
        assertAllBounded(historyReads);
    }

    @Test
    void stakeRefresh_isOneBoundedLookupPerAddress_withNoWindowFunction() {
        Random rnd = new Random(8L);
        List<StakeRow> history = new ArrayList<>();
        List<String> addresses = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            String stake = PREFIX + "stake_" + i;
            addresses.add(stake);
            history.addAll(stakeHistory(rnd, stake, distinctSlots(rnd, 6, SLOT - 1_000, SLOT + 1_000)));
        }
        insertStakeHistory(dsl, history);
        upsertStakeCurrent(dsl, latestStake(history).values());

        executed.clear();
        storage.refreshCurrentStakeAddressBalance(addresses, SLOT);
        List<String> sql = lower(executed);
        System.out.println("refreshCurrentStakeAddressBalance SQL (" + sql.size() + " statements), first: " + sql.get(0));

        assertNoWindowFunction(sql);
        List<String> historyReads = matching(sql, READS_STAKE_HISTORY);
        assertEquals(addresses.size(), historyReads.size(),
                "one stake_address_balance lookup per address, got:\n" + String.join("\n", historyReads));
        assertAllBounded(historyReads);
    }

    @Test
    void refresh_withNothingAtOrBeforeTheSlot_deletesAndIssuesNoInsert() {
        Random rnd = new Random(9L);
        String address = PREFIX + "late_addr";
        String stake = PREFIX + "late_stake";
        List<AddrRow> addrHist = new ArrayList<>();
        for (String unit : List.of("lovelace", "tok"))
            addrHist.addAll(addrHistory(rnd, address, unit, distinctSlots(rnd, 5, SLOT + 1, SLOT + 1_000), null));
        List<StakeRow> stakeHist = stakeHistory(rnd, stake, distinctSlots(rnd, 5, SLOT + 1, SLOT + 1_000));
        insertAddrHistory(dsl, addrHist);
        insertStakeHistory(dsl, stakeHist);
        upsertAddrCurrent(dsl, latestAddr(addrHist).values());
        upsertStakeCurrent(dsl, latestStake(stakeHist).values());

        executed.clear();
        storage.refreshCurrentAddressBalance(address, Set.of("lovelace", "tok"), SLOT);
        storage.refreshCurrentStakeAddressBalance(List.of(stake), SLOT);
        List<String> sql = lower(executed);

        assertTrue(readAddrCurrent(dsl, PREFIX).isEmpty(), "address current rows removed");
        assertTrue(readStakeCurrent(dsl, PREFIX).isEmpty(), "stake current rows removed");
        assertEquals(1, matching(sql, Pattern.compile("^delete from address_balance_current\\b")).size(), String.join("\n", sql));
        assertEquals(1, matching(sql, Pattern.compile("^delete from stake_address_balance_current\\b")).size(), String.join("\n", sql));
        assertEquals(List.of(), matching(sql, Pattern.compile("\\binsert\\b")),
                "nothing to restore, so no insert may be issued (and never one with an empty values list)");
    }

    private static List<String> lower(List<String> statements) {
        return statements.stream().map(s -> s.toLowerCase(Locale.ROOT).trim()).toList();
    }

    private static List<String> matching(List<String> statements, Pattern pattern) {
        return statements.stream().filter(s -> pattern.matcher(s).find()).collect(Collectors.toList());
    }

    private static void assertNoWindowFunction(List<String> statements) {
        List<String> windowed = matching(statements, WINDOW);
        assertEquals(List.of(), windowed, "a window over a key's whole balance history is the slow form (#1351)");
    }

    private static void assertAllBounded(List<String> historyReads) {
        Stream<String> unbounded = historyReads.stream().filter(s -> !ROW_LIMIT.matcher(s).find());
        assertEquals(List.of(), unbounded.toList(), "every balance-history read must stop at its first row");
        historyReads.forEach(s -> assertTrue(s.contains("order by") && s.contains("desc"),
                "the lookup must take the newest row: " + s));
    }
}
