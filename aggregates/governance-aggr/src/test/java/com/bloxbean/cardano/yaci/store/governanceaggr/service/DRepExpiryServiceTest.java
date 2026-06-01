package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovEpochActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DRepExpiryServiceTest {

    private static final String DREP_HASH = "967c86ac16aea79dea7609c8b186f91c259a458a00e7d4ed3a7f9d3a";

    @Mock
    private EraService eraService;
    @Mock
    private GovEpochActivityRepository govEpochActivityRepository;

    private JdbcTemplate jdbcTemplate;
    private DRepExpiryService dRepExpiryService;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        jdbcTemplate = new JdbcTemplate(dataSource);
        var namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        var dsl = DSL.using(dataSource, SQLDialect.H2);

        dRepExpiryService = new DRepExpiryService(
                namedJdbcTemplate,
                eraService,
                govEpochActivityRepository,
                dsl,
                new ObjectMapper());

        createSchema();

        when(eraService.getEras()).thenReturn(List.of(CardanoEra.builder()
                .era(Era.Conway)
                .startSlot(42_508_827L)
                .build()));
        when(eraService.getEpochNo(Era.Conway, 42_508_827L)).thenReturn(492);
    }

    @Test
    void activeUntilTreatsRatifiedProposalStatusEpochAsNonDormant() {
        when(govEpochActivityRepository.findDormantEpochsInEpochRange(492, 580))
                .thenReturn(Set.of());

        insertEpochParam(492, 9, 20, 60);
        insertEpochParam(493, 9, 20, 60);
        insertEpochParam(580, 9, 20, 60);

        insertDRepRegistration();
        insertProposal("proposal-at-era-start", 492, 42_510_119L);
        insertProposalStatus("proposal-at-era-start", 493, "RATIFIED");
        insertProposal("proposal-flush", 580, 50_184_338L);
        insertDRepDist(581);

        dRepExpiryService.calculateAndUpdateExpiryForEpoch(581);

        Integer activeUntil = jdbcTemplate.queryForObject("""
                SELECT active_until
                FROM drep_dist
                WHERE epoch = 581
                  AND drep_hash = ?
                """, Integer.class, DREP_HASH);

        assertThat(activeUntil).isEqualTo(600);
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE drep_dist (
                    drep_hash varchar(56),
                    drep_type varchar(40),
                    amount bigint,
                    epoch int,
                    active_until int,
                    expiry int,
                    primary key (drep_hash, drep_type, epoch)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE drep_registration (
                    tx_hash varchar(64) not null,
                    cert_index int not null,
                    tx_index int not null,
                    type varchar(50),
                    drep_hash varchar(56),
                    cred_type varchar(40),
                    epoch int,
                    slot bigint,
                    primary key (tx_hash, cert_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE voting_procedure (
                    voter_hash varchar(56),
                    voter_type varchar(40),
                    epoch int,
                    slot bigint,
                    tx_index int,
                    idx int
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE gov_action_proposal (
                    tx_hash varchar(64),
                    idx int,
                    epoch int,
                    slot bigint,
                    tx_index int
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE gov_action_proposal_status (
                    gov_action_tx_hash varchar(64),
                    gov_action_index int,
                    type varchar(50),
                    status varchar(50),
                    epoch int
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE epoch_param (
                    epoch int primary key,
                    params varchar
                )
                """);
    }

    private void insertEpochParam(int epoch, int protocolMajorVersion, int dRepActivity, int govActionLifetime) {
        jdbcTemplate.update("""
                INSERT INTO epoch_param (epoch, params)
                VALUES (?, ?)
                """, epoch, """
                {"protocol_major_ver":%d,"drep_activity":%d,"gov_action_lifetime":%d}
                """.formatted(protocolMajorVersion, dRepActivity, govActionLifetime));
    }

    private void insertDRepRegistration() {
        jdbcTemplate.update("""
                INSERT INTO drep_registration
                    (tx_hash, cert_index, tx_index, type, drep_hash, cred_type, epoch, slot)
                VALUES ('drep-reg', 0, 0, 'REG_DREP_CERT', ?, 'ADDR_KEYHASH', 493, 42596698)
                """, DREP_HASH);
    }

    private void insertProposal(String txHash, int epoch, long slot) {
        jdbcTemplate.update("""
                INSERT INTO gov_action_proposal
                    (tx_hash, idx, epoch, slot, tx_index)
                VALUES (?, 0, ?, ?, 0)
                """, txHash, epoch, slot);
    }

    private void insertProposalStatus(String txHash, int epoch, String status) {
        jdbcTemplate.update("""
                INSERT INTO gov_action_proposal_status
                    (gov_action_tx_hash, gov_action_index, type, status, epoch)
                VALUES (?, 0, 'PARAMETER_CHANGE_ACTION', ?, ?)
                """, txHash, status, epoch);
    }

    private void insertDRepDist(int epoch) {
        jdbcTemplate.update("""
                INSERT INTO drep_dist
                    (drep_hash, drep_type, amount, epoch)
                VALUES (?, 'ADDR_KEYHASH', 1, ?)
                """, DREP_HASH, epoch);
    }
}
