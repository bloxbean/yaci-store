package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import com.bloxbean.cardano.yaci.store.common.util.ErrorCode;
import com.bloxbean.cardano.yaci.store.events.ErrorEvent;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepositSnapshotServiceTest {
    private DepositSnapshotService depositSnapshotService;
    private JdbcTemplate jdbcTemplate;
    private DepositParamService depositParamService;
    private ApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:deposit_snapshot;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        depositParamService = mock(DepositParamService.class);
        publisher = mock(ApplicationEventPublisher.class);
        depositSnapshotService = new DepositSnapshotService(
                new NamedParameterJdbcTemplate(dataSource), depositParamService, publisher);

        jdbcTemplate.execute("DROP TABLE IF EXISTS stake_registration");
        jdbcTemplate.execute("DROP TABLE IF EXISTS pool");
        jdbcTemplate.execute("""
                CREATE TABLE stake_registration (
                    tx_hash VARCHAR(128),
                    type VARCHAR(64),
                    deposit NUMERIC(38, 0),
                    address VARCHAR(255),
                    slot BIGINT,
                    tx_index INTEGER,
                    cert_index INTEGER,
                    epoch INTEGER
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE pool (
                    tx_hash VARCHAR(128),
                    status VARCHAR(64),
                    amount NUMERIC(38, 0),
                    epoch INTEGER
                )
                """);
    }

    @Test
    void getNetStakeDepositInEpoch_usesStoredStakeDepositsAndExcludesSyntheticGenesisRows() {
        jdbcTemplate.update("""
                INSERT INTO stake_registration(tx_hash, type, deposit, epoch)
                VALUES
                    ('Genesis', 'STAKE_REGISTRATION', 9000000, 0),
                    ('normal-reg', 'STAKE_REGISTRATION', 5000000, 0),
                    ('normal-dereg', 'STAKE_DEREGISTRATION', 2000000, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO pool(tx_hash, status, amount, epoch)
                VALUES
                    ('Genesis', 'REGISTRATION', 500000000, 0),
                    ('normal-pool', 'REGISTRATION', 700000000, 0),
                    ('retired-pool', 'RETIRED', 100000000, 1)
                """);

        BigInteger netDeposit = depositSnapshotService.getNetStakeDepositInEpoch(0);

        assertThat(netDeposit).isEqualTo(BigInteger.valueOf(603_000_000L));
    }

    @Test
    void getNetStakeDepositInEpoch_resolvesLegacyDeregistrationFromActiveRegistration() {
        jdbcTemplate.update("""
                INSERT INTO stake_registration(
                    tx_hash, type, deposit, address, slot, tx_index, cert_index, epoch)
                VALUES
                    ('registration', 'STAKE_REGISTRATION', 5000000, 'stake1', 100, 0, 0, 0),
                    ('legacy-deregistration', 'STAKE_DEREGISTRATION', NULL, 'stake1', 200, 0, 0, 1)
                """);

        BigInteger netDeposit = depositSnapshotService.getNetStakeDepositInEpoch(1);

        assertThat(netDeposit).isEqualTo(BigInteger.valueOf(-5_000_000));
    }

    @Test
    void getNetStakeDepositInEpoch_resolvesLifecycleOrderWithinTransaction() {
        jdbcTemplate.update("""
                INSERT INTO stake_registration(
                    tx_hash, type, deposit, address, slot, tx_index, cert_index, epoch)
                VALUES
                    ('same-tx', 'STAKE_REGISTRATION', 5000000, 'stake1', 200, 3, 0, 1),
                    ('same-tx', 'STAKE_DEREGISTRATION', NULL, 'stake1', 200, 3, 1, 1)
                """);

        BigInteger netDeposit = depositSnapshotService.getNetStakeDepositInEpoch(1);

        assertThat(netDeposit).isZero();
    }

    @Test
    void getNetStakeDepositInEpoch_prefersExplicitDeregistrationDeposit() {
        jdbcTemplate.update("""
                INSERT INTO stake_registration(
                    tx_hash, type, deposit, address, slot, tx_index, cert_index, epoch)
                VALUES
                    ('registration', 'STAKE_REGISTRATION', 5000000, 'stake1', 100, 0, 0, 0),
                    ('explicit-deregistration', 'STAKE_DEREGISTRATION', 7000000, 'stake1', 200, 0, 0, 1)
                """);

        BigInteger netDeposit = depositSnapshotService.getNetStakeDepositInEpoch(1);

        assertThat(netDeposit).isEqualTo(BigInteger.valueOf(-7_000_000));
    }

    @Test
    void getNetStakeDepositInEpoch_usesProtocolParameterFallbackAndReportsMissingLifecycle() {
        jdbcTemplate.update("""
                INSERT INTO stake_registration(
                    tx_hash, type, deposit, address, slot, tx_index, cert_index, epoch)
                VALUES
                    ('legacy-deregistration-1', 'STAKE_DEREGISTRATION', NULL, 'stake1', 200, 0, 0, 1),
                    ('legacy-deregistration-2', 'STAKE_DEREGISTRATION', NULL, 'stake2', 201, 0, 0, 1)
                """);
        when(depositParamService.getKeyDeposit(1)).thenReturn(BigInteger.valueOf(2_000_000));

        BigInteger netDeposit = depositSnapshotService.getNetStakeDepositInEpoch(1);

        assertThat(netDeposit).isEqualTo(BigInteger.valueOf(-4_000_000));
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getErrorCode()).isEqualTo(ErrorCode.DATA_MISSING_ERROR.name());
        assertThat(eventCaptor.getValue().getReason())
                .isEqualTo("Unable to resolve 2 legacy stake deregistration deposit(s) for epoch 1");
        assertThat(eventCaptor.getValue().getDetails())
                .isEqualTo("Using protocol parameter key deposit fallback: 4000000");
    }

    @Test
    void getNetStakeDepositInEpoch_doesNotCrossPreviousDeregistrationLifecycle() {
        jdbcTemplate.update("""
                INSERT INTO stake_registration(
                    tx_hash, type, deposit, address, slot, tx_index, cert_index, epoch)
                VALUES
                    ('registration', 'STAKE_REGISTRATION', 5000000, 'stake1', 100, 0, 0, 0),
                    ('first-deregistration', 'STAKE_DEREGISTRATION', 5000000, 'stake1', 150, 0, 0, 0),
                    ('second-deregistration', 'STAKE_DEREGISTRATION', NULL, 'stake1', 200, 0, 0, 1)
                """);
        when(depositParamService.getKeyDeposit(1)).thenReturn(BigInteger.valueOf(2_000_000));

        BigInteger netDeposit = depositSnapshotService.getNetStakeDepositInEpoch(1);

        assertThat(netDeposit).isEqualTo(BigInteger.valueOf(-2_000_000));
        verify(publisher).publishEvent(org.mockito.ArgumentMatchers.any(ErrorEvent.class));
    }
}
