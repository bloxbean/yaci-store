package com.bloxbean.cardano.yaci.store.core.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresIdleTransactionTimeoutPostProcessorTest {

    @Test
    void setsTheTimeoutOnPostgresPools() {
        HikariDataSource dataSource = hikari("jdbc:postgresql://localhost:5432/yaci_store");

        new PostgresIdleTransactionTimeoutPostProcessor(1800).postProcessAfterInitialization(dataSource, "dataSource");

        assertThat(dataSource.getConnectionInitSql()).isEqualTo("SET idle_in_transaction_session_timeout = '1800s'");
    }

    @Test
    void leavesOtherDatabasesAlone() {
        HikariDataSource h2 = hikari("jdbc:h2:mem:test");
        HikariDataSource duckdb = hikari("jdbc:duckdb:/tmp/analytics.duckdb");

        var postProcessor = new PostgresIdleTransactionTimeoutPostProcessor(1800);
        postProcessor.postProcessAfterInitialization(h2, "dataSource");
        postProcessor.postProcessAfterInitialization(duckdb, "duckDbWriterDataSource");

        assertThat(h2.getConnectionInitSql()).isNull();
        assertThat(duckdb.getConnectionInitSql()).isNull();
    }

    @Test
    void keepsAConfiguredConnectionInitSql() {
        HikariDataSource dataSource = hikari("jdbc:postgresql://localhost:5432/yaci_store");
        dataSource.setConnectionInitSql("SET search_path = preview");

        new PostgresIdleTransactionTimeoutPostProcessor(1800).postProcessAfterInitialization(dataSource, "dataSource");

        assertThat(dataSource.getConnectionInitSql()).isEqualTo("SET search_path = preview");
    }

    @Test
    void zeroTurnsItOff() {
        HikariDataSource dataSource = hikari("jdbc:postgresql://localhost:5432/yaci_store");

        new PostgresIdleTransactionTimeoutPostProcessor(0).postProcessAfterInitialization(dataSource, "dataSource");

        assertThat(dataSource.getConnectionInitSql()).isNull();
    }

    private static HikariDataSource hikari(String url) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        return dataSource;
    }
}
