package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DRepPv9ClearEventCacheServiceTest {
    private static final int PV9_MAX_EPOCH = 3;
    private static final String OLD_DREP = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String NEW_DREP = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String THIRD_DREP = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    private JdbcTemplate jdbcTemplate;
    private DRepPv9ClearEventCacheService service;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        jdbcTemplate = new JdbcTemplate(dataSource);
        service = new DRepPv9ClearEventCacheService(new NamedParameterJdbcTemplate(dataSource));

        createSchema();
    }

    @Test
    void rebuildCache_capturesCredentialDRepStaleReverseClearEvent() {
        // Happy path for the PV9 stale reverse-entry issue:
        // the address first delegates to old_drep, then re-delegates to new_drep,
        // but old_drep may still keep the address in its reverse set. When old_drep
        // unregisters, ledger clears the address's forward delegation.
        insertDRepRegistration("old-reg", "REG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 1, 10, 0);
        insertDelegation("old-del", "stake-clear", OLD_DREP, "ADDR_KEYHASH", 2, 12, 0);
        insertDRepRegistration("new-reg", "REG_DREP_CERT", NEW_DREP, "ADDR_KEYHASH", 2, 15, 0);
        insertDelegation("new-del", "stake-clear", NEW_DREP, "ADDR_KEYHASH", 2, 20, 0);
        insertDRepRegistration("old-unreg", "UNREG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 3, 30, 0);

        service.rebuildCache(PV9_MAX_EPOCH);

        // The persistent cache stores the immutable clear event. It does not mean the
        // address is excluded forever; snapshot-specific after_del is checked separately.
        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event")).isEqualTo(1L);
        assertThat(queryForString("SELECT address FROM drep_pv9_stale_clear_event"))
                .isEqualTo("stake-clear");
    }

    @Test
    void rebuildCache_ignoresDelegationBeforeOldDRepRegistration() {
        // A delegation to old_drep before old_drep was registered cannot create a ledger
        // reverse entry for old_drep. Therefore old_drep's later unregistration must not
        // be interpreted as a PV9 stale clear for this address.
        insertDelegation("old-del-before-reg", "stake-before-reg", OLD_DREP, "ADDR_KEYHASH", 1, 10, 0);
        insertDRepRegistration("old-reg", "REG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 2, 15, 0);
        insertDelegation("new-del", "stake-before-reg", NEW_DREP, "ADDR_KEYHASH", 2, 20, 0);
        insertDRepRegistration("old-unreg", "UNREG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 3, 30, 0);

        service.rebuildCache(PV9_MAX_EPOCH);

        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event")).isZero();
    }

    @Test
    void rebuildCache_ignoresDirectVirtualDelegationThatAlreadyRemovedOldDRep() {
        // ABSTAIN/NO_CONFIDENCE only suppress the stale-clear event when they were the
        // actual current delegation transition away from old_drep. In that case ledger
        // already removed old_drep normally, so old_drep's later unregistration should
        // not produce another clear event.
        insertDRepRegistration("old-reg", "REG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 1, 10, 0);
        insertDelegation("old-del", "stake-virtual", OLD_DREP, "ADDR_KEYHASH", 2, 12, 0);
        insertDelegation("abstain-del", "stake-virtual", null, "ABSTAIN", 2, 20, 0);
        insertDRepRegistration("old-unreg", "UNREG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 3, 30, 0);

        service.rebuildCache(PV9_MAX_EPOCH);

        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event")).isZero();
    }

    @Test
    void snapshotClearedAddresses_excludesEventAfterLaterDelegation() {
        // The cache stores a clear event at old_drep unregistration. For a snapshot
        // before any later delegation, the address is still considered cleared and must
        // appear in ss_pv9_cleared_addresses.
        insertDRepRegistration("old-reg", "REG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 1, 10, 0);
        insertDelegation("old-del", "stake-redelegates", OLD_DREP, "ADDR_KEYHASH", 2, 12, 0);
        insertDelegation("new-del", "stake-redelegates", NEW_DREP, "ADDR_KEYHASH", 2, 20, 0);
        insertDRepRegistration("old-unreg", "UNREG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 3, 30, 0);
        service.rebuildCache(PV9_MAX_EPOCH);

        service.createSnapshotClearedAddressesTable("", 3, PV9_MAX_EPOCH);
        assertThat(snapshotClearedAddresses()).containsExactly("stake-redelegates");

        jdbcTemplate.execute("DROP TABLE ss_pv9_cleared_addresses");
        insertDelegation("third-del", "stake-redelegates", THIRD_DREP, "ADDR_KEYHASH", 4, 40, 0);

        // Once the address delegates again after the clear event, ledger has a fresh
        // forward delegation. The per-snapshot after_del check must stop excluding it.
        service.createSnapshotClearedAddressesTable("", 4, PV9_MAX_EPOCH);
        assertThat(snapshotClearedAddresses()).isEmpty();
    }

    @Test
    void rollbackInvalidation_keepsCacheWhenRollbackIsAfterPv9SourceMaxSlot() {
        // The marker stores the max slot of the PV9 source rows used to build the cache.
        // A rollback after that slot cannot change the cached PV9 source history, so the
        // cache should be kept.
        insertDRepRegistration("old-reg", "REG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 1, 10, 0);
        insertDelegation("old-del", "stake-rollback", OLD_DREP, "ADDR_KEYHASH", 2, 12, 0);
        insertDelegation("new-del", "stake-rollback", NEW_DREP, "ADDR_KEYHASH", 2, 20, 0);
        insertDRepRegistration("old-unreg", "UNREG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 3, 30, 0);
        service.ensureCacheReady(PV9_MAX_EPOCH);

        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event_cache")).isEqualTo(1L);
        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event")).isEqualTo(1L);
        assertThat(count("SELECT pv9_source_max_slot FROM drep_pv9_stale_clear_event_cache")).isEqualTo(30L);

        service.handleRollbackEvent(rollbackEvent(31));

        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event_cache")).isEqualTo(1L);
        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event")).isEqualTo(1L);
    }

    @Test
    void rollbackInvalidation_clearsMarkerAndEventsWhenRollbackTouchesPv9SourceThenRebuilds() {
        // Rolling back to the cached PV9 source max slot can remove or change rows that
        // were used to build the cache. The marker and events must be cleared so the next
        // post-bootstrap snapshot rebuilds from the current source tables.
        insertDRepRegistration("old-reg", "REG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 1, 10, 0);
        insertDelegation("old-del", "stake-rollback", OLD_DREP, "ADDR_KEYHASH", 2, 12, 0);
        insertDelegation("new-del", "stake-rollback", NEW_DREP, "ADDR_KEYHASH", 2, 20, 0);
        insertDRepRegistration("old-unreg", "UNREG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 3, 30, 0);
        service.ensureCacheReady(PV9_MAX_EPOCH);

        service.handleRollbackEvent(rollbackEvent(30));

        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event_cache")).isZero();
        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event")).isZero();

        service.ensureCacheReady(PV9_MAX_EPOCH);

        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event_cache")).isEqualTo(1L);
        assertThat(count("SELECT COUNT(*) FROM drep_pv9_stale_clear_event")).isEqualTo(1L);
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE delegation_vote (
                    tx_hash varchar(64) not null,
                    cert_index int not null,
                    tx_index int not null,
                    address varchar(255),
                    drep_hash varchar(56),
                    drep_id varchar(255),
                    drep_type varchar(40),
                    epoch int,
                    credential varchar(56),
                    cred_type varchar(40),
                    slot bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, cert_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE drep_registration (
                    tx_hash varchar(64) not null,
                    cert_index int not null,
                    tx_index int not null,
                    type varchar(50),
                    deposit bigint,
                    drep_hash varchar(56),
                    drep_id varchar(255),
                    cred_type varchar(40),
                    epoch int,
                    slot bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, cert_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE stake_registration (
                    tx_hash varchar(64) not null,
                    cert_index int not null,
                    tx_index int not null,
                    type varchar(50),
                    address varchar(255),
                    epoch int,
                    slot bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, cert_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE drep_pv9_stale_clear_event_cache (
                    pv9_max_epoch int,
                    event_count bigint not null,
                    pv9_source_max_slot bigint not null,
                    update_datetime timestamp,
                    primary key (pv9_max_epoch)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE drep_pv9_stale_clear_event (
                    pv9_max_epoch int not null,
                    address varchar(255) not null,
                    old_drep_hash varchar(56) not null,
                    old_drep_type varchar(40) not null,
                    stale_slot bigint not null,
                    stale_tx_index int not null,
                    stale_cert_index int not null,
                    unreg_epoch int not null,
                    unreg_slot bigint not null,
                    unreg_tx_index int not null,
                    unreg_cert_index int not null,
                    update_datetime timestamp
                )
                """);
    }

    private void insertDRepRegistration(String txHash, String type, String drepHash, String credType, int epoch, long slot, int certIndex) {
        jdbcTemplate.update("""
                INSERT INTO drep_registration
                    (tx_hash, cert_index, tx_index, type, deposit, drep_hash, drep_id, cred_type, epoch, slot)
                VALUES (?, ?, 0, ?, 0, ?, ?, ?, ?, ?)
                """, txHash, certIndex, type, drepHash, drepHash, credType, epoch, slot);
    }

    private void insertDelegation(String txHash, String address, String drepHash, String drepType, int epoch, long slot, int certIndex) {
        jdbcTemplate.update("""
                INSERT INTO delegation_vote
                    (tx_hash, cert_index, tx_index, address, drep_hash, drep_id, drep_type, epoch, credential, cred_type, slot)
                VALUES (?, ?, 0, ?, ?, ?, ?, ?, ?, 'ADDR_KEYHASH', ?)
                """, txHash, certIndex, address, drepHash, drepHash, drepType, epoch, address, slot);
    }

    private long count(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    private String queryForString(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private List<String> snapshotClearedAddresses() {
        return jdbcTemplate.queryForList("""
                SELECT address
                FROM ss_pv9_cleared_addresses
                ORDER BY address
                """, String.class);
    }

    private RollbackEvent rollbackEvent(long slot) {
        return RollbackEvent.builder()
                .rollbackTo(new Point(slot, "rollback-hash"))
                .build();
    }
}
