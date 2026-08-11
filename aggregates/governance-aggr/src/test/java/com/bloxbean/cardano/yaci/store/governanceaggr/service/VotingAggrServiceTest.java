package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.DREP_REGISTRATION;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.VOTING_PROCEDURE;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_DIST;
import static org.assertj.core.api.Assertions.assertThat;

class VotingAggrServiceTest {
    private static final int SNAPSHOT_EPOCH = 10;
    private static final String DREP_HASH = "11111111111111111111111111111111111111111111111111111111";
    private static final String GOV_ACTION_TX_HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private DSLContext dsl;
    private VotingAggrService votingAggrService;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        dsl = DSL.using(dataSource, SQLDialect.H2, new Settings().withRenderQuotedNames(RenderQuotedNames.NEVER));
        votingAggrService = new VotingAggrService(dsl, null, null);
        createSchema();
        insertDRepDistribution(SNAPSHOT_EPOCH + 1);
    }

    @Test
    void getVotesByDRep_doesNotReviveVoteFromBeforeReregistration() {
        insertVote("old-vote", Vote.YES, 8, 100, 1, 0);
        insertRegistration("unregister", CertificateType.UNREG_DREP_CERT, 9, 100, 2, 0);
        insertRegistration("reregister", CertificateType.REG_DREP_CERT, 9, 100, 3, 0);

        assertThat(getVotes()).isEmpty();
    }

    @Test
    void getVotesByDRep_returnsNewVoteCastAfterReregistration() {
        insertVote("old-vote", Vote.YES, 8, 100, 1, 0);
        insertRegistration("unregister", CertificateType.UNREG_DREP_CERT, 9, 100, 2, 0);
        insertRegistration("reregister", CertificateType.REG_DREP_CERT, 9, 100, 3, 0);
        insertVote("new-vote", Vote.NO, 9, 100, 4, 0);

        assertThat(getVotes()).singleElement().satisfies(vote -> {
            assertThat(vote.getTxHash()).isEqualTo("new-vote");
            assertThat(vote.getVote()).isEqualTo(Vote.NO);
        });
    }

    @Test
    void getVotesByDRep_ignoresUnregistrationAfterSnapshotEpoch() {
        insertVote("historical-vote", Vote.YES, SNAPSHOT_EPOCH, 100, 1, 0);
        insertRegistration("future-unregister", CertificateType.UNREG_DREP_CERT, SNAPSHOT_EPOCH + 1, 101, 0, 0);

        assertThat(getVotes()).singleElement().satisfies(vote -> {
            assertThat(vote.getTxHash()).isEqualTo("historical-vote");
            assertThat(vote.getVote()).isEqualTo(Vote.YES);
        });
    }

    private List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> getVotes() {
        var govActionId = GovActionId.builder()
                .transactionId(GOV_ACTION_TX_HASH)
                .gov_action_index(0)
                .build();
        return votingAggrService.getVotesByDRep(SNAPSHOT_EPOCH, List.of(govActionId));
    }

    private void insertVote(String txHash, Vote vote, int epoch, long slot, int txIndex, int index) {
        dsl.insertInto(VOTING_PROCEDURE)
                .set(VOTING_PROCEDURE.ID, UUID.randomUUID())
                .set(VOTING_PROCEDURE.TX_HASH, txHash)
                .set(VOTING_PROCEDURE.IDX, index)
                .set(VOTING_PROCEDURE.TX_INDEX, txIndex)
                .set(VOTING_PROCEDURE.VOTER_TYPE, VoterType.DREP_KEY_HASH.name())
                .set(VOTING_PROCEDURE.VOTER_HASH, DREP_HASH)
                .set(VOTING_PROCEDURE.GOV_ACTION_TX_HASH, GOV_ACTION_TX_HASH)
                .set(VOTING_PROCEDURE.GOV_ACTION_INDEX, 0)
                .set(VOTING_PROCEDURE.VOTE, vote.name())
                .set(VOTING_PROCEDURE.EPOCH, epoch)
                .set(VOTING_PROCEDURE.SLOT, slot)
                .execute();
    }

    private void insertRegistration(String txHash, CertificateType type, int epoch, long slot, int txIndex, int certIndex) {
        dsl.insertInto(DREP_REGISTRATION)
                .set(DREP_REGISTRATION.TX_HASH, txHash)
                .set(DREP_REGISTRATION.CERT_INDEX, certIndex)
                .set(DREP_REGISTRATION.TX_INDEX, txIndex)
                .set(DREP_REGISTRATION.TYPE, type.name())
                .set(DREP_REGISTRATION.DREP_HASH, DREP_HASH)
                .set(DREP_REGISTRATION.CRED_TYPE, StakeCredType.ADDR_KEYHASH.name())
                .set(DREP_REGISTRATION.EPOCH, epoch)
                .set(DREP_REGISTRATION.SLOT, slot)
                .execute();
    }

    private void insertDRepDistribution(int epoch) {
        dsl.insertInto(DREP_DIST)
                .set(DREP_DIST.DREP_HASH, DREP_HASH)
                .set(DREP_DIST.DREP_TYPE, StakeCredType.ADDR_KEYHASH.name())
                .set(DREP_DIST.AMOUNT, 1_000L)
                .set(DREP_DIST.EPOCH, epoch)
                .execute();
    }

    private void createSchema() {
        dsl.execute("""
                CREATE TABLE voting_procedure (
                    id uuid not null,
                    tx_hash varchar(64) not null,
                    idx int not null,
                    tx_index int not null,
                    voter_type varchar(50),
                    voter_hash varchar(56),
                    gov_action_tx_hash varchar(64),
                    gov_action_index int,
                    vote varchar(10),
                    anchor_url varchar,
                    anchor_hash varchar(64),
                    epoch int,
                    slot bigint,
                    block bigint,
                    block_time bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, voter_hash, voter_type, gov_action_tx_hash, gov_action_index)
                )
                """);
        dsl.execute("""
                CREATE TABLE drep_registration (
                    tx_hash varchar(64) not null,
                    cert_index int not null,
                    tx_index int not null,
                    type varchar(50),
                    deposit bigint,
                    drep_hash varchar(56),
                    drep_id varchar(255),
                    anchor_url varchar,
                    anchor_hash varchar(64),
                    cred_type varchar(40),
                    epoch int,
                    slot bigint,
                    block bigint,
                    block_time bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, cert_index)
                )
                """);
        dsl.execute("""
                CREATE TABLE drep_dist (
                    drep_hash varchar(56),
                    drep_type varchar(40),
                    drep_id varchar(255),
                    amount bigint,
                    epoch int,
                    active_until int,
                    expiry int,
                    update_datetime timestamp,
                    primary key (drep_hash, drep_type, epoch)
                )
                """);
    }
}
