package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DepositSnapshotServiceTest {
    private DepositSnapshotService depositSnapshotService;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:deposit_snapshot;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        depositSnapshotService = new DepositSnapshotService(new NamedParameterJdbcTemplate(dataSource));

        jdbcTemplate.execute("DROP TABLE IF EXISTS stake_registration");
        jdbcTemplate.execute("DROP TABLE IF EXISTS pool");
        jdbcTemplate.execute("""
                CREATE TABLE stake_registration (
                    tx_hash VARCHAR(128),
                    type VARCHAR(64),
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
    void getNetStakeDepositInEpoch_excludesSyntheticGenesisRowsOnly() {
        jdbcTemplate.update("""
                INSERT INTO stake_registration(tx_hash, type, epoch)
                VALUES
                    ('Genesis', 'STAKE_REGISTRATION', 0),
                    ('normal-reg', 'STAKE_REGISTRATION', 0),
                    ('normal-dereg', 'STAKE_DEREGISTRATION', 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO pool(tx_hash, status, amount, epoch)
                VALUES
                    ('Genesis', 'REGISTRATION', 500000000, 0),
                    ('normal-pool', 'REGISTRATION', 700000000, 0),
                    ('retired-pool', 'RETIRED', 100000000, 1)
                """);

        BigInteger netDeposit = depositSnapshotService.getNetStakeDepositInEpoch(0);

        assertThat(netDeposit).isEqualTo(BigInteger.valueOf(600_000_000L));
    }
}
