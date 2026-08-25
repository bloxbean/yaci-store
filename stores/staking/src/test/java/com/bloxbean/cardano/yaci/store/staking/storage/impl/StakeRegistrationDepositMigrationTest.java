package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StakeRegistrationDepositMigrationTest {

    @Test
    void migrationAddsDepositColumnAndBackfillsExistingRows() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:stake_registration_deposit_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        new ResourceDatabasePopulator(
                new ClassPathResource("db/store/h2/V0_800_1__init.sql"))
                .execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("""
                INSERT INTO stake_registration(tx_hash, cert_index, credential, type, epoch)
                VALUES
                    ('registration', 0, 'credential-1', 'STAKE_REGISTRATION', 10),
                    ('deregistration', 0, 'credential-1', 'STAKE_DEREGISTRATION', 20)
                """);

        new ResourceDatabasePopulator(
                new ClassPathResource("db/store/h2/V0_800_3__stake_registration_deposit.sql"))
                .execute(dataSource);

        List<BigInteger> deposits = jdbcTemplate.queryForList(
                        "SELECT deposit FROM stake_registration ORDER BY epoch", BigInteger.class);
        assertThat(deposits).containsExactly(
                BigInteger.valueOf(2_000_000),
                BigInteger.valueOf(2_000_000));
    }
}
