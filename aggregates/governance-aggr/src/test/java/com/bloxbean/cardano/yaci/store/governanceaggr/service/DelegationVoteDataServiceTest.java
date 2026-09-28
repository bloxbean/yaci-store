package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DelegationVoteDataServiceTest {

    private static final int SNAPSHOT_EPOCH = 312;
    private static final String REWARD_ACCOUNT = "stake_test1_reward_account";
    private static final String OTHER_REWARD_ACCOUNT = "stake_test1_other_reward_account";
    private static final String DREP = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String STAKE_REGISTRATION = "STAKE_REGISTRATION";
    private static final String STAKE_DEREGISTRATION = "STAKE_DEREGISTRATION";

    private JdbcTemplate jdbcTemplate;
    private DelegationVoteDataService delegationVoteDataService;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        jdbcTemplate = new JdbcTemplate(dataSource);
        delegationVoteDataService = new DelegationVoteDataService(DSL.using(dataSource, SQLDialect.H2));

        createSchema();
    }

    @Test
    void shouldReturnDelegationWhenAccountRemainsRegistered() {
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 256, 2_000L, 0, 0);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).containsExactly(REWARD_ACCOUNT);
    }

    @Test
    void shouldDropDelegationWhenAccountDeregisteredAfterDelegating() {
        // Preprod reward account from issue #1148: always-abstain in epoch 256, deregistered in
        // epoch 290, so at epoch 312 the pools using it must fall back to the No default.
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 256, 2_000L, 0, 0);
        registration(STAKE_DEREGISTRATION, 290, 3_000L, 0, 0);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).isEmpty();
    }

    @Test
    void shouldDropDelegationWhenAccountReRegisteredWithoutNewDelegation() {
        // Re-registration creates a fresh account with no DRep delegation; it cannot revive the
        // delegation that the deregistration removed.
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 256, 2_000L, 0, 0);
        registration(STAKE_DEREGISTRATION, 290, 3_000L, 0, 0);
        registration(STAKE_REGISTRATION, 295, 4_000L, 0, 0);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).isEmpty();
    }

    @Test
    void shouldReturnDelegationIssuedAfterReRegistration() {
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 256, 2_000L, 0, 0);
        registration(STAKE_DEREGISTRATION, 290, 3_000L, 0, 0);
        registration(STAKE_REGISTRATION, 295, 4_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 296, 5_000L, 0, 0);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).containsExactly(REWARD_ACCOUNT);
    }

    @Test
    void shouldDropDelegationWhenDeregistrationIsALaterCertificateOfTheSameTransaction() {
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 300, 2_000L, 4, 0);
        registration(STAKE_DEREGISTRATION, 300, 2_000L, 4, 1);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).isEmpty();
    }

    @Test
    void shouldReturnDelegationWhenItIsALaterCertificateOfTheSameTransaction() {
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        registration(STAKE_DEREGISTRATION, 300, 2_000L, 4, 0);
        registration(STAKE_REGISTRATION, 300, 2_000L, 4, 1);
        delegation(DrepType.ABSTAIN, 300, 2_000L, 4, 2);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).containsExactly(REWARD_ACCOUNT);
    }

    @Test
    void shouldDropDelegationWhenDeregistrationIsALaterTransactionOfTheSameSlot() {
        // Ordering within a slot is by transaction index, so a deregistration in a later
        // transaction of the same block still invalidates the delegation.
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 300, 2_000L, 1, 0);
        registration(STAKE_DEREGISTRATION, 300, 2_000L, 2, 0);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).isEmpty();
    }

    @Test
    void shouldIgnoreDeregistrationAfterSnapshotEpoch() {
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 256, 2_000L, 0, 0);
        registration(STAKE_DEREGISTRATION, SNAPSHOT_EPOCH + 1, 9_000L, 0, 0);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).containsExactly(REWARD_ACCOUNT);
    }

    @Test
    void shouldDropNoConfidenceDelegationWhenAccountDeregistered() {
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.NO_CONFIDENCE, 256, 2_000L, 0, 0);
        registration(STAKE_DEREGISTRATION, 290, 3_000L, 0, 0);

        assertThat(addressesWithDefault(DrepType.NO_CONFIDENCE)).isEmpty();
    }

    @Test
    void shouldNotReportVirtualDefaultWhenLatestDelegationIsToARegularDRep() {
        registration(STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(DrepType.ABSTAIN, 256, 2_000L, 0, 0);
        delegation(DrepType.ADDR_KEYHASH, 260, 3_000L, 0, 0);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).isEmpty();
    }

    @Test
    void shouldResolveEachAccountIndependently() {
        registration(REWARD_ACCOUNT, STAKE_REGISTRATION, 250, 1_000L, 0, 0);
        delegation(REWARD_ACCOUNT, DrepType.ABSTAIN, 256, 2_000L, 0, 0);
        registration(REWARD_ACCOUNT, STAKE_DEREGISTRATION, 290, 3_000L, 0, 0);

        registration(OTHER_REWARD_ACCOUNT, STAKE_REGISTRATION, 250, 1_100L, 0, 0);
        delegation(OTHER_REWARD_ACCOUNT, DrepType.ABSTAIN, 256, 2_100L, 0, 0);

        assertThat(addressesWithDefault(DrepType.ABSTAIN)).containsExactly(OTHER_REWARD_ACCOUNT);
    }

    private List<String> addressesWithDefault(DrepType drepType) {
        return delegationVoteDataService
                .getDelegationVotesByDRepTypeAndAddressList(List.of(REWARD_ACCOUNT, OTHER_REWARD_ACCOUNT), drepType, SNAPSHOT_EPOCH)
                .stream()
                .map(DelegationVote::getAddress)
                .toList();
    }

    private void delegation(DrepType drepType, int epoch, long slot, int txIndex, int certIndex) {
        delegation(REWARD_ACCOUNT, drepType, epoch, slot, txIndex, certIndex);
    }

    private void delegation(String address, DrepType drepType, int epoch, long slot, int txIndex, int certIndex) {
        boolean virtualDRep = drepType == DrepType.ABSTAIN || drepType == DrepType.NO_CONFIDENCE;

        jdbcTemplate.update("""
                INSERT INTO delegation_vote
                    (tx_hash, cert_index, tx_index, address, drep_hash, drep_id, drep_type, credential, cred_type, epoch, slot)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ADDR_KEYHASH', ?, ?)
                """,
                txHash("del", slot, txIndex, certIndex), certIndex, txIndex, address,
                virtualDRep ? null : DREP, virtualDRep ? null : DREP, drepType.name(),
                address, epoch, slot);
    }

    private void registration(String type, int epoch, long slot, int txIndex, int certIndex) {
        registration(REWARD_ACCOUNT, type, epoch, slot, txIndex, certIndex);
    }

    private void registration(String address, String type, int epoch, long slot, int txIndex, int certIndex) {
        jdbcTemplate.update("""
                INSERT INTO stake_registration
                    (tx_hash, cert_index, tx_index, credential, cred_type, type, address, epoch, slot)
                VALUES (?, ?, ?, ?, 'ADDR_KEYHASH', ?, ?, ?, ?)
                """,
                txHash("reg", slot, txIndex, certIndex), certIndex, txIndex, address, type, address, epoch, slot);
    }

    private String txHash(String prefix, long slot, int txIndex, int certIndex) {
        return prefix + "-" + slot + "-" + txIndex + "-" + certIndex;
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
                    block bigint,
                    block_time bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, cert_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE stake_registration (
                    tx_hash varchar(64) not null,
                    cert_index int not null,
                    tx_index int,
                    credential varchar(56) not null,
                    cred_type varchar(50),
                    type varchar(50),
                    address varchar(255),
                    epoch int,
                    slot bigint,
                    block_hash varchar(64),
                    block bigint,
                    block_time bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, cert_index)
                )
                """);
    }
}
