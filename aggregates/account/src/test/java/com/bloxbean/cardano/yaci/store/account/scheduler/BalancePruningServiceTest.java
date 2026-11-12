package com.bloxbean.cardano.yaci.store.account.scheduler;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.config.JooqTestConfig;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JooqTestConfig.class})
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/store/h2",
        "spring.flyway.out-of-order=true",
        "spring.datasource.url=jdbc:h2:mem:pruningdb",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//Pruning deletes in its own REQUIRES_NEW transactions, so the test data must be committed
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BalancePruningServiceTest {
    private static final long CUTOFF = 1000;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Flyway flyway;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private BalancePruningService pruningService;

    @BeforeAll
    void migrate() {
        flyway.migrate();
    }

    @BeforeEach
    void setUp() {
        dsl.execute("delete from address_balance");
        dsl.execute("delete from stake_address_balance");
        pruningService = new BalancePruningService(null, null, new AccountStoreProperties(), dsl, transactionManager);
    }

    @Test
    void addressBalance_keepsLatestSettledRow_whenOnlyNewerRowIsInRollbackWindow() {
        insertAddressBalance("addr_x", 500);
        insertAddressBalance("addr_x", 1500); //still inside the rollback window

        pruningService.performAddressBalanceDelete(CUTOFF);

        assertThat(addressBalanceSlots("addr_x")).containsExactly(500L, 1500L);
    }

    @Test
    void addressBalance_deletesOlderRows_butKeepsLatestSettledRow() {
        insertAddressBalance("addr_x", 300);
        insertAddressBalance("addr_x", 500);
        insertAddressBalance("addr_x", 900);
        insertAddressBalance("addr_x", 1500);

        pruningService.performAddressBalanceDelete(CUTOFF);

        assertThat(addressBalanceSlots("addr_x")).containsExactly(900L, 1500L);
    }

    @Test
    void stakeAddressBalance_keepsLatestSettledRow_whenOnlyNewerRowIsInRollbackWindow() {
        insertStakeAddressBalance("stake_x", 500);
        insertStakeAddressBalance("stake_x", 1500);

        pruningService.performStakeAddressBalanceDelete(CUTOFF);

        assertThat(stakeAddressBalanceSlots("stake_x")).containsExactly(500L, 1500L);
    }

    @Test
    void stakeAddressBalance_deletesOlderRows_butKeepsLatestSettledRow() {
        insertStakeAddressBalance("stake_x", 300);
        insertStakeAddressBalance("stake_x", 500);
        insertStakeAddressBalance("stake_x", 900);
        insertStakeAddressBalance("stake_x", 1500);

        pruningService.performStakeAddressBalanceDelete(CUTOFF);

        assertThat(stakeAddressBalanceSlots("stake_x")).containsExactly(900L, 1500L);
    }

    private void insertAddressBalance(String address, long slot) {
        dsl.execute("insert into address_balance (address, unit, slot, quantity) values (?, 'lovelace', ?, 100)", address, slot);
    }

    private void insertStakeAddressBalance(String address, long slot) {
        dsl.execute("insert into stake_address_balance (address, slot, quantity) values (?, ?, 100)", address, slot);
    }

    private List<Long> addressBalanceSlots(String address) {
        return dsl.fetch("select slot from address_balance where address = ? order by slot", address)
                .getValues(0, Long.class);
    }

    private List<Long> stakeAddressBalanceSlots(String address) {
        return dsl.fetch("select slot from stake_address_balance where address = ? order by slot", address)
                .getValues(0, Long.class);
    }
}
